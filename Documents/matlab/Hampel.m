% Загрузка данных
spectrum = importdata('fon.dat'); 

% Применение фильтра с оптимальными параметрами
[filtered, outliers] = hampel_filter(spectrum, 9, 3);

% Комбинирование с MLS-сглаживанием
lambda = 200; % Параметр сглаживания
smoothed = poisson_smooth_mls(filtered, lambda, 3);


% Визуализация
subplot(2,1,1)
plot(spectrum, 'b')
title('Исходный спектр')

subplot(2,2,2)
plot(smoothed, 'r')
title('MLS-сглаживание')
xlabel('Канал')

% Визуализация
subplot(2,2,3)
plot(log(spectrum), 'b')
title('Исходный спектр LOG')

subplot(2,2,4)
plot(log(smoothed), 'r')
title('MLS-сглаживание LOG')
xlabel('Канал')


function [y_filtered, outliers] = hampel_filter(y, window_size, k_sigma)
% HAMPEL_FILTER Адаптивный фильтр для подавления выбросов в спектрах
%   y - входной спектр (вектор)
%   window_size - размер скользящего окна (рекомендуется 5-15)
%   k_sigma - пороговое число стандартных отклонений (рекомендуется 2.5-3.5)
%
%   y_filtered - отфильтрованный спектр
%   outliers - логическая маска выбросов

% Проверка аргументов
if nargin < 2
    window_size = 7; % Оптимум для спектров 2048 каналов
end
if nargin < 3
    k_sigma = 3.0; % Стандартный порог для гамма-спектроскопии
end

% Инициализация
n = length(y);
y_filtered = y(:); % Гарантируем вектор-столбец
outliers = false(n, 1);
half_win = floor(window_size/2);

% Константа для нормального распределения
gaussian_const = 1.4826; % MAD -> STD для нормального распределения

% Главный цикл обработки
for i = 1:n
    % Определяем окно с обработкой краев
    start_idx = max(1, i - half_win);
    end_idx = min(n, i + half_win);
    window = y(start_idx:end_idx);
    
    % Вычисляем медиану и MAD
    med = median(window);
    mad = median(abs(window - med));
    
    % Вычисляем адаптивный порог
    threshold = k_sigma * gaussian_const * mad;
    
    % Обнаружение выброса
    if abs(y(i) - med) > threshold
        outliers(i) = true;
        y_filtered(i) = med; % Замена на медиану
        
        % Дополнительное сглаживание для высокочастотных выбросов
        if i > 1 && i < n
            % Взвешенная интерполяция с соседями
            weights = [0.25, 0.5, 0.25]; % Трехточечный фильтр
            neighbors = [y_filtered(i-1), y_filtered(i), y_filtered(i+1)];
            y_filtered(i) = sum(weights .* neighbors);
        end
    end
end

% Визуализация результатов (опционально)
if nargout == 0
    figure('Name', 'Фильтр Хампеля');
    subplot(2,1,1);
    plot(y, 'b'); hold on;
    plot(y_filtered, 'r', 'LineWidth', 1.5);
    title('Сравнение спектров');
    legend('Исходный', 'Фильтрованный');
    xlabel('Канал'); ylabel('Интенсивность');
    
    subplot(2,1,2);
    stem(outliers, 'filled', 'MarkerSize', 3);
    title('Обнаруженные выбросы');
    xlabel('Канал'); ylim([-0.1 1.1]);
    set(gcf, 'Color', 'w');
end
end


function smoothed = poisson_smooth_mls(y, lambda, d, max_iter, tol)
% POISSON_SMOOTH_MLS Сглаживание спектров методом максимального правдоподобия
%   y - исходный спектр (вектор-столбец)
%   lambda - параметр сглаживания (10-1000)
%   d - порядок разностей (рекомендуется 2)
%   max_iter - максимум итераций (по умолчанию 50)
%   tol - критерий остановки (по умолчанию 1e-5)

% Проверка входных параметров
if nargin < 3, d = 2; end
if nargin < 4, max_iter = 50; end
if nargin < 5, tol = 1e-5; end

% Инициализация с защитой от нулевых значений
n = length(y);
mu = y + 0.25;  % Псевдосчёт для нулевых каналов

% Построение матрицы регуляризации
E = speye(n);  % Разреженная единичная матрица
D = diff(E, d);  % Матрица разностей d-го порядка
R = lambda * (D' * D);  % Матрица штрафа

% Итеративный алгоритм
for iter = 1:max_iter
    mu_old = mu;
    
    % Веса и рабочий отклик
    w = max(mu, 1e-3);  % Защита от отрицательных значений
    z = log(w) + (y - mu) ./ w;
    
    % Решение системы уравнений
    W = spdiags(w, 0, n, n);  % Разреженная диагональная матрица весов
    A = W + R;
    b = W * z;
    
    % Решение СЛАУ методом сопряженных градиентов
    theta = pcg(A, b, 1e-6, 100);  
    
    % Обновление оценки
    mu = exp(theta);
    
    % Проверка сходимости
    delta = norm(mu - mu_old) / norm(mu);
    if delta < tol
        break;
    end
end

smoothed = mu;
end