channels = 1:1024;
spectrum = importdata('fon.dat');

% Параметры алгоритма
min_peak_height = 20;       % Минимальная высота пика над фоном
%levels = [1, 2, 3];      % Уровни декомпозиции для анализа
levels = [3, 4, 5];      % Уровни декомпозиции для анализа
%levels = [1, 2, 3, 4, 5];      % Уровни декомпозиции для анализа
threshold_factor = 0.05;     % Пороговый коэффициент

% Вызов функции

peak_positions = wavelet_peak_detection_db5(spectrum, min_peak_height, levels, threshold_factor);