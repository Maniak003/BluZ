clear, clc, close all;

% Загрузка данных
data = importdata('fon.dat');
signal = data(:); % Преобразование в столбец

% Параметры сигнала
Fs = 1000;          % Частота дискретизации (укажите вашу)
N = length(signal); % Длина сигнала
t = (0:N-1)/Fs;     % Вектор времени

% Прямое преобразование Фурье
Y = fft(signal);

% Вектор частот (двусторонний спектр со сдвигом)
f = (-Fs/2 : Fs/N : Fs/2 - Fs/N)'; 

% Сдвигаем спектр для удобства
Y_shifted = fftshift(Y);

% Задайте диапазон пропускания [f_low, f_high]
f_low = 0;    % Нижняя граница (Гц)
f_high = 50;  % Верхняя граница (Гц)

% Создаем маску: 1 для частот в диапазоне [f_low, f_high]
mask = (abs(f) >= f_low) & (abs(f) <= f_high);

% Применяем маску к спектру
Y_filtered = Y_shifted .* mask;

% Обратный сдвиг спектра
Y_filtered = ifftshift(Y_filtered);

% Обратное преобразование Фурье
filtered_signal = real(ifft(Y_filtered));
log_signal = log(filtered_signal);

% Визуализация
figure;

% Исходный сигнал и спектр
plot(t, log(signal));
title('Исходный сигнал');

%figure;
%plot(t, filtered_signal);
%title('Сигнал после полосового фильтра');

figure;
plot(t, log_signal);