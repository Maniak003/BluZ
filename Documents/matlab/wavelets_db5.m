% Предположим, что C — это ваш сигнал.
C = importdata('fon.dat');
maxLevel = 5; % Максимальный уровень разложения
waveletType = 'db5'; % Тип вейвлета

% Выполняем вейвлет-разложение
[C, L] =  wavedec(C, maxLevel, waveletType); 

% Извлекаем континуума на основе предыдущего значения
continuum = mean(C); 

% Проверяем размерность
if length(C) ~= 1024
    C = resample(C, 1024, length(C)); % Скорректируйте размеры, если это необходимо
end

% Сглаживание спектра
spectrumSmooth = medfilt1(C, 15); 

% Построение графика с числом каналов
channels = 1:length(spectrumSmooth); % Генерация вектора каналов

figure;
plot(channels, spectrumSmooth, 'b-', 'LineWidth', 1.5); % Построение сглаженного спектра
hold on;
plot(channels, repmat(continuum, size(spectrumSmooth, 1), 1), 'g--', 'LineWidth', 1.5); % Визуализация континуума
title('Gamma Spectrum with Detected Peaks');
xlabel('Channel Number');
ylabel('Intensity');

% Поиск пиков на каждом уровне разложения
thresholdDetail = 0.1; % Порог для детализирующих коэффициентов; настройте при необходимости
thresholdAmplitude = 2; % Порог для амплитуды

% Цикл по уровням
for level = 1:maxLevel
    detailCoeffs = detcoef(C, L, level); % Извлечение детализирующих коэффициентов на уровне level
    peaks = find(abs(detailCoeffs) > thresholdDetail); % Повышенное значение

    % Наносим пики на график
    for i = peaks'
        % Поскольку мы получаем коэффициенты, которые могут быть меньше длины signal, 
        % добавляем проверку
        if i <= length(spectrumSmooth) && abs(detailCoeffs(i)) > thresholdAmplitude * continuum
            plot(channels(i), spectrumSmooth(i), 'ro', 'MarkerFaceColor', 'r'); % Помечаем пики
            fprintf('Level %d: Detected peak at index %d with amplitude %.2f\n', level, i, abs(detailCoeffs(i)));
        end
    end
end

hold off;
legend('Gamma Spectrum', 'Continuum', 'Detected Peaks');