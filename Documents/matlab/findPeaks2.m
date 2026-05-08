channels = 1:1024;
spectrum = importdata('fon.dat');

% Параметры алгоритма
min_peak_height = 40;    % Минимальная высота пика
scale_range = [1, 50];   % Диапазон масштабов (3-20 каналов)
threshold_factor = 0.1;  % Пороговый коэффициент (30% от макс. значения)

% Вызов функции
peak_positions = wavelet_peak_detection(spectrum, min_peak_height, scale_range, threshold_factor);
peak_positions = wavelet_peak_detection_db5(spectrum, min_peak_height, levels, threshold_factor);