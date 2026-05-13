% readGeant4File.m
clear; clc;

% 1. Чтение данных (пропускаем 7 строк заголовка Geant4)
data = readmatrix('response_800keV.csv', 'NumHeaderLines', 7);

% 2. Извлекаем ТОЛЬКО 1024 рабочих бина
%    Строка 1 = underflow, Строка 1026 = overflow
counts = data(2:end-1, 1);

% 3. Рассчитываем центры бинов для 1024 ячеек (диапазон 0–3.5 МэВ)
bin_centers = ((0:1023)' + 0.5) * 3.5 / 1024;

% 4. Проверка размеров (должно быть 1024x1 для обоих)
fprintf('Размер bin_centers: %d x %d\n', size(bin_centers));
fprintf('Размер counts:      %d x %d\n', size(counts));

% 5. Находим отсчёты в районе 0.662 МэВ
[~, idx] = min(abs(bin_centers - 0.662));
fprintf('Отсчёты в бине 0.800 МэВ: %d\n', counts(idx));

% 6. Визуализация (теперь размеры совпадают)
figure;
plot(bin_centers, counts, 'b-', 'LineWidth', 1.5);
xlabel('Energy (MeV)'); ylabel('Counts');
title('NaI(Tl) 10x10 Response: 800 keV');
xlim([0 1.5]); grid on;