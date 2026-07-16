%% build_and_export_chi.m
%% CsI(Tl) 10x10x40
% Полный пайплайн: N энергий → матрица отклика → весовой вектор → бинарный файл для Android
% Формат выхода: 1024 × double (IEEE 754, Little-Endian) = 8192 байт

clear; clc; close all;

%% ==================== ЧТЕНИЕ config.txt ====================
cfg_file = 'config.txt';
if ~exist(cfg_file, 'file')
    error('Файл %s не найден! Убедитесь, что он лежит в текущей директории MATLAB.', cfg_file);
end

fid = fopen(cfg_file, 'r');
while ~feof(fid)
    line = fgetl(fid);
    if ~ischar(line), continue; end
    line = strtrim(line);
    if isempty(line) || line(1) == '#', continue; end

    % Корректное разделение по '='
    parts = strsplit(line, '=');
    if numel(parts) < 2, continue; end

    key = strtrim(parts{1});
    val = strtrim(parts{2});
    val = strrep(val, char(13), ''); % Убираем \r (Windows-окончания строк)

    switch key
        case 'ENERGIES'
            energies_keV = str2num(val);
        case 'N_EVENTS'
            N_EVENTS = str2double(val);
        case 'LAMBDA'
            LAMBDA = str2double(val);
        case 'OUTPUT_FILE'
            OUTPUT_FILE = val;
        case 'ENERGY_MIN'
            ENERGY_MIN = str2double(val);
        case 'ENERGY_MAX'
            ENERGY_MAX = str2double(val);
        case 'N_BINS'
            N_BINS = str2double(val);
    end
end
fclose(fid);

% 🔍 Проверка загрузки критических параметров (fail-fast)
if ~exist('N_BINS', 'var') || isnan(N_BINS) || N_BINS == 0
    error('Ошибка парсинга config.txt: N_BINS не загружен или равен 0/NaN.');
end
if ~exist('energies_keV', 'var') || isempty(energies_keV)
    error('Ошибка парсинга config.txt: список ENERGIES пуст. Проверьте формат строки.');
end
if ~exist('N_EVENTS', 'var') || isnan(N_EVENTS)
    error('Ошибка парсинга config.txt: N_EVENTS не задан.');
end

% Вспомогательные переменные
ENERGY_RANGE = [ENERGY_MIN, ENERGY_MAX];
bin_width = (ENERGY_RANGE(2) - ENERGY_RANGE(1)) / N_BINS;
bin_centers = (ENERGY_RANGE(1) + (0:N_BINS-1)' + 0.5) * bin_width;

fprintf('Конфигурация загружена: %d энергий, N=%.0f, λ=%.1e, bins=%d\n', ...
    numel(energies_keV), N_EVENTS, LAMBDA, N_BINS);

% ВАЖНО: Коэффициенты перехода флюенс → доза (пример для H*(10), мкР·см²)
% Замените на данные из ICRP-74 / ICRP-116 под величину дозы
E_REF = [30, 50, 80, 100, 150, 200, 300, 400, 500, 662, 1000, 1332, 2000, 2614, 3000, 3500, 4000, 5000, 6000, 7000, 8000];
K_REF = [0.65, 0.85, 1.12, 1.28, 1.65, 1.92, 2.35, 2.68, 2.95, 3.18, 3.78, 3.95, 4.25, 4.45, 4.55, 4.65, 4.75, 4.85, 4.95, 5.00, 5.05];
%K_REF = [0.65, 0.85, 1.12, 1.28, 1.65, 1.92, 2.35, 2.68, 2.95, 3.18, 3.78, 3.95, 4.25, 4.45, 4.55, 4.65];

% Калибровка (опционально)
DO_CALIBRATION = false;
if DO_CALIBRATION
    CALIB_ENERGY = 662;
    CALIB_DOSE_KNOWN = 12.5;  % мкР/ч
    CALIB_SPECTRUM_FILE = 'measured_Cs137.csv';
end

%% ==================== 1. ЗАГРУЗКА МАТРИЦЫ ====================
fprintf('=== Загрузка %d файлов отклика ===\n', numel(energies_keV));

R = zeros(N_BINS, numel(energies_keV));

for i = 1:numel(energies_keV)
    E = energies_keV(i);
    % Формат имени: точка заменяется на подчёркивание (как в bash-скрипте)
    E_NAME = strrep(sprintf('%g', E), '.', '_');
    fname = sprintf('response_%skeV.csv', E_NAME);
    
    if ~exist(fname, 'file')
        warning('Файл не найден: %s', fname);
        continue;
    end
    
    % Чтение CSV Geant4 h1d формата (пропускаем 7 строк заголовка)
    data = readmatrix(fname, 'NumHeaderLines', 7);
    
    % Извлечение 1024 рабочих бинов (строки 2:(end-1), столбец 1 = entries)
    counts = data(2:end-1, 1);
    
    % Нормировка на число событий → вероятность регистрации
    R(:, i) = counts / N_EVENTS;
    
    % Лог прогресса
    [peak_val, peak_idx] = max(R(:,i));
    fprintf('[%2d/%2d] %6.1f keV: max=%.2e при %.3f МэВ (бин %4d)\n', ...
        i, numel(energies_keV), E, peak_val, bin_centers(peak_idx), peak_idx);
end

fprintf('Матрица R загружена: размер %d × %d\n', size(R));

%% ==================== 2. РАСЧЁТ ВЕСОВОГО ВЕКТОРА ====================
fprintf('\n=== Расчёт весового вектора ===\n');

% Интерполяция коэффициентов k(E) на сетку энергий матрицы
k = interp1(E_REF, K_REF, energies_keV, 'pchip', 'extrap');
k = k(:);  % столбец [N_energies × 1]

%figure; plot(energies_keV/1000, k, 'o-'); grid;
%xlabel('Energy (MeV)'); ylabel('Dose coefficient');

% Вариант №1
%n_energies = size(R, 2);
%A = R' * R + LAMBDA * eye(n_energies);  % матрица [N_energies × N_energies]
%w = R * (A \ k);                        % весовой вектор [1024 × 1]
% Пост-сглаживание (убирает пики, не ломая физику)
%windowSize = 9;
%kernel = ones(windowSize, 1) / windowSize;
%w = conv(w, kernel, 'same');
% Принудительный рост весов выше 1 МэВ (физическое ограничение)
%for i = 1:N_BINS
%    if bin_centers(i) > 1.0
%        w(i) = max(w(i), 0.5 * i / N_BINS * max(w));  % минимальный рост
%    end
%end

% Вариант №2 (оптимальный)
% Матрица второй разности (штрафует резкие скачки весов)
%D2 = diff(eye(N_BINS), 2);
%lambda = LAMBDA;
%A = R * R' + lambda * (D2' * D2);
%w = A \ (R * k);
%w(w < 0) = 0;

% Вариант №3
% Регуляризованная NNLS: min ||[R'; sqrt(LAMBDA)*I] * w - [k; 0]||^2  с w>=0
%C = [R'; sqrt(LAMBDA) * eye(N_BINS)];
%d = [k; zeros(N_BINS,1)];
%w = lsqnonneg(C, d);
%fprintf('Вектор весов рассчитан: диапазон [%.3e, %.3e]\n', min(w), max(w));

%Вариант №4
% Регуляризация Тихонова (базовый расчёт)
%n_energies = size(R, 2);
%A = R' * R + LAMBDA * eye(n_energies);
%w_raw = R * (A \ k);  % весовой вектор [1024 × 1]
%fprintf('Вектор весов рассчитан: диапазон [%.3e, %.3e]\n', min(w_raw), max(w_raw));
% Сглаживание (убираем артефакты высоких энергий)
% Используем гауссово окно, ширина которого растет с энергией
%w_smooth = zeros(N_BINS, 1);
%sigma_base = 2.0;  % базовая ширина в бинах
%for i = 1:N_BINS
%    E = bin_centers(i);
    % Ширина сглаживания: от 2 бинов (низкие энергии) до 12 бинов (высокие)
%    sigma = sigma_base + 10 * max(0, (E - 1.0) / 2.5);
    
    % Гауссово ядро
%    half_win = ceil(3 * sigma);
%    idx = max(1, i-half_win) : min(N_BINS, i+half_win);
%    x = (idx - i);
%    kernel = exp(-x.^2 / (2*sigma^2));
%    kernel = kernel / sum(kernel);
    
%    w_smooth(i) = sum(w_raw(idx) .* kernel');
%end
%w = w_smooth;

% Вариант №5
% Адаптивная регуляризация: усиливаем λ для высоких энергий
%n_energies = size(R, 2);
%lambda_vec = LAMBDA * ones(n_energies, 1);
% Для энергий >1.5 МэВ увеличиваем регуляризацию в 10 раз
%idx_high = energies_keV > 1500;
%lambda_vec(idx_high) = LAMBDA * 10;
% Создаем диагональную матрицу
%A = R' * R + diag(lambda_vec);
%w = R * (A \ k);
% Пост-сглаживание (более агрессивное для высоких энергий)
%windowSize = 21;  % увеличили с 15 до 21
%kernel = ones(windowSize, 1) / windowSize;
%w = conv(w, kernel, 'same');

% Принудительное сглаживание хвоста
%for i = 1:N_BINS
%    if bin_centers(i) > 2.0
%        % Плавное затухание осцилляций
%        t = (bin_centers(i) - 2.0) / 1.5;
%        smooth_factor = 0.7 + 0.3 * t;  % от 0.7 до 1.0
%        if i > 1 && i < N_BINS
%            w(i) = smooth_factor * w(i) + (1-smooth_factor) * 0.5*(w(i-1)+w(i+1));
%        end
%    end
%end

% Вариант №6
% min ||R'w - k||² + λ_s ||D2 w||²   при   w >= 0
n = N_BINS;
D2 = diff(eye(n), 2);                       % (n-2) x n
lambda_s = LAMBDA ;                         % выбрать по L-кривой, см. ниже

% Сводим к NNLS форме: min ||C*w - d||²,  w >= 0
C = [R'; sqrt(lambda_s) * D2];           % (n_E + n - 2) x n
d = [k; zeros(n-2, 1)];

w = lsqnonneg(C, d);
% 4. Принудительное обнуление отрицательных весов (физически невозможны)
%w(w < 0) = 0;
%fprintf('Вектор весов сглажен и очищен: диапазон [%.3e, %.3e]\n', min(w), max(w));
%fprintf('Вес при 0.1 МэВ:  %.3f (ожидается: 0.3–1.0)\n', w(35));
%fprintf('Вес при 0.66 МэВ: %.3f (ожидается: 2–8)\n', w(226));
%fprintf('Вес при 2.0 МэВ:  %.3f (ожидается: 10–40)\n', w(685));
%fprintf('Вес при 3.0 МэВ:  %.3f (ожидается: 20–60)\n', w(1024));

fprintf('  Сумма весов: %.3f, λ = %.1e\n', sum(w), LAMBDA);
% (Опционально) Калибровка
if DO_CALIBRATION && exist(CALIB_SPECTRUM_FILE, 'file')
    fprintf('\n=== Калибровка по %d keV ===\n', CALIB_ENERGY);
    spec_data = readmatrix(CALIB_SPECTRUM_FILE);
    spectrum_meas = spec_data(:, min(2, size(spec_data,2)));  % берём отсчёты
    if length(spectrum_meas) ~= N_BINS
        error('Спектр должен иметь %d значений', N_BINS);
    end
    dose_calc = w' * spectrum_meas;
    scale = CALIB_DOSE_KNOWN / dose_calc;
    w = w * scale;
    fprintf('Калибровка: масштаб = %.4f\n', scale);
end

% Реконструкция для проверки сходимости.
%
%k_reconstructed = R' * w;
%figure; plot(energies_keV/1000, k, 'ro-', energies_keV/1000, k_reconstructed, 'bs-');
%legend('Target k(E)', 'Reconstructed k(E)');

% Подбор LAMBDA (версия №1 - медлено)
%lambdas = logspace(-6, 2, 30);
%resid = zeros(size(lambdas));
%sol_curv = zeros(size(lambdas));

%for i = 1:numel(lambdas)
%    C = [R'; sqrt(lambdas(i)) * D2];
%    d = [k; zeros(n-2, 1)];
%    w_i = lsqnonneg(C, d);
    
%    resid(i) = norm(R'*w_i - k);          % ошибка сходимости
%    sol_curv(i) = norm(D2 * w_i);         % «изгибность» решения
%end

% Подбор LAMBDA (версия №2)
%D2 = diff(speye(N_BINS), 2);       % разреженная, размер (N_BINS-2) × N_BINS
%L = D2' * D2;                      % матрица штрафа, N_BINS × N_BINS
%RtR = R * R';                      % N_BINS × N_BINS

%lambdas = logspace(-6, 2, 30);
%resid = zeros(size(lambdas));
%sol_curv = zeros(size(lambdas));

%for i = 1:numel(lambdas)
%    A = RtR + lambdas(i) * L;
%    w = A \ (R * k);               % R*k имеет размер N_BINS × 1
%    %w(w < 0) = 0;                 % обрезаем отрицательные (опционально)
%    
%    resid(i) = norm(R' * w - k);   % невязка восстановления k
%    sol_curv(i) = norm(D2 * w);    % гладкость (вторая производная)
%end

% Выбор λ по L-кривой
%log_res = log10(resid);
%log_curv = log10(sol_curv);
% Кривизна как вторая производная
%curvature = diff(diff(log_res) ./ diff(log_curv));
%[~, idx_opt] = max(curvature);
%lambda_opt = lambdas(idx_opt + 1);
%fprintf('Оптимальный Lambda: %d\n', lambda_opt);

%figure;
%loglog(resid, sol_curv, 'o-');
%hold on;
%for i = 1:numel(lambdas)
%    text(resid(i), sol_curv(i), sprintf(' %.1e', lambdas(i)), 'FontSize', 8);
%end
%xlabel('||R''w - k||');
%ylabel('||D_2 w||');

%figure; loglog(resid, sol_curv, 'o-'); grid on;
%xlabel('R''w - k'); ylabel('D_2 w');
%% ==================== 3. ЭКСПОРТ В БИНАРНЫЙ ФОРМАТ ====================
fprintf('\n=== Экспорт вектора для Android ===\n');

output_name = OUTPUT_FILE; % Берём из конфига
[~, name, ~] = fileparts(output_name);
if isempty(name), name = output_name; end
filename_bin = [name '.chi'];

% Запись в бинарный файл (Little-Endian, IEEE 754 double)
fid = fopen(filename_bin, 'w', 'ieee-le');
if fid == -1
    error('Не удалось открыть файл: %s', filename_bin);
end
count = fwrite(fid, w(:), 'double');
fclose(fid);

if count ~= N_BINS
    error('Записано %d из %d значений', count, N_BINS);
end

fileSize = dir(filename_bin).bytes;
expectedSize = N_BINS * 8;
fprintf('Файл сохранён: %s\n', filename_bin);
fprintf('  Размер: %d байт (ожидалось: %d) %s\n', ...
    fileSize, expectedSize, ternary(fileSize==expectedSize, 'Ok', 'Bad'));
fprintf('  Диапазон: [%.3e, %.3e]\n', min(w), max(w));

%% ==================== 4. ПРОВЕРКА ЧТЕНИЯ ====================
fprintf('\n=== Проверка записи/чтения ===\n');
fid = fopen(filename_bin, 'r', 'ieee-le');
w_check = fread(fid, N_BINS, 'double');
fclose(fid);

max_diff = max(abs(w - w_check));
fprintf('Макс. расхождение: %.3e %s\n', max_diff, ternary(max_diff<1e-15, 'Ok', 'Bad'));

%% ==================== 5. ВИЗУАЛИЗАЦИЯ ====================
figure('Name', 'Chi Vector', 'Position', [100,100,1000,400]);
plot(bin_centers, w, 'b-', 'LineWidth', 1);
xlabel('Energy (MeV)'); ylabel('Weight');
title('Chi Vector: CsI(Tl) 10×10x30 mm');
grid on; xlim([0, 3.5]);

%% ==================== 6. СОХРАНЕНИЕ ДЛЯ MATLAB ====================
save('dose_weights_CsI_10x10x30.mat', 'w', 'bin_centers', 'LAMBDA', 'energies_keV');
fprintf('\n=== Готово ===\n');
fprintf('Бинарный файл: %s (загрузите в Android через loadChiFile)\n', filename_bin);
fprintf('MAT-файл: dose_weights_CsI_10x10x30.mat (для отладки в MATLAB)\n');

%% ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================
function out = ternary(cond, a, b)
    if cond, out = a; else, out = b; end
end