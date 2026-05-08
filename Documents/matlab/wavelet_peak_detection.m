function peak_positions = wavelet_peak_detection_db5(spectrum, min_peak_height, levels, threshold_factor)
    % spectrum: входной спектр (1024 канала)
    % min_peak_height: минимальная высота пика НАД ФОНОМ
    % levels: уровни декомпозиции для анализа (например, [1, 2, 3])
    % threshold_factor: порог для коэффициентов детализации (0-1)
    
    % 1. Подготовка данных
    spectrum = spectrum(:)';  % Преобразуем в вектор-строку
    L = length(spectrum);
    
    % 2. Оценка фона с помощью медианного фильтра
    background_window = 51; % Размер окна для оценки фона (нечетный)
    background = medfilt1(spectrum, background_window, 'truncate');
    
    % 3. Вейвлет-разложение
    wavelet = 'db5';
    max_level = max(levels);
    [C, L] = wavedec(spectrum, max_level, wavelet);
    
    % 4. Реконструкция детализирующих коэффициентов для выбранных уровней
    detail_sum = zeros(size(spectrum));
    
    for lev = levels
        D = wrcoef('d', C, L, wavelet, lev);
        detail_sum = detail_sum + abs(D);
    end
    
    % 5. Нормировка и пороговая обработка
    detail_norm = detail_sum / max(detail_sum);
    threshold = threshold_factor * max(detail_norm);
    candidate_mask = (detail_norm > threshold);
    
    % 6. Поиск пиков (локальных максимумов в нормированной сумме деталей)
    [pks, locs] = findpeaks(detail_norm .* candidate_mask, 'MinPeakHeight', threshold);
    
    % 7. Фильтрация по высоте над фоном
    net_heights = spectrum(locs) - background(locs);
    height_mask = (net_heights > min_peak_height);
    peak_positions = locs(height_mask);
    
    % 8. Фильтрация по расстоянию (сохраняем самый высокий пик в группе)
    [peak_positions, ~] = filter_close_peaks(spectrum, background, peak_positions, 10);
    
    % 9. Визуализация результатов
    figure('Position', [100, 100, 1200, 800]);
    
    % Исходный спектр с пиками и фоном
    subplot(3,1,1);
    plot(spectrum, 'b', 'LineWidth', 1.5);
    hold on;
    plot(background, 'k--', 'LineWidth', 1.5);
    plot(peak_positions, spectrum(peak_positions), 'ro', 'MarkerSize', 8, 'LineWidth', 2);
    title(['Обнаружено пиков: ', num2str(length(peak_positions))]);
    xlabel('Канал');
    ylabel('Интенсивность');
    grid on;
    legend('Спектр', 'Фон', 'Найденные пики');
    
    % Детализирующие коэффициенты
    subplot(3,1,2);
    colors = lines(length(levels));
    hold on;
    for i = 1:length(levels)
        lev = levels(i);
        D = wrcoef('d', C, L, wavelet, lev);
        plot(abs(D), 'Color', colors(i,:), 'LineWidth', 1.5);
    end
    plot(detail_norm, 'k', 'LineWidth', 2);
    yline(threshold, '--r', 'Порог');
    title('Детализирующие коэффициенты');
    xlabel('Канал');
    ylabel('Амплитуда');
    legend_str = arrayfun(@(x) ['Ур. ', num2str(x)], levels, 'UniformOutput', false);
    legend([legend_str, {'Сумма', 'Порог'}]);
    grid on;
    
    % Нормированная сумма деталей с порогом
    subplot(3,1,3);
    plot(detail_norm, 'b', 'LineWidth', 1.5);
    hold on;
    plot(locs, pks, 'ro', 'MarkerSize', 8);
    yline(threshold, '--r', 'Порог');
    title('Нормированная сумма деталей и кандидаты в пики');
    xlabel('Канал');
    ylabel('Нормированная амплитуда');
    grid on;
    
    % Вывод результатов
    fprintf('Найдено пиков: %d\n', length(peak_positions));
    disp('Позиции пиков (каналы):');
    disp(peak_positions);
end

function [filtered_positions, filtered_heights] = filter_close_peaks(spectrum, background, positions, min_distance)
    if isempty(positions)
        filtered_positions = [];
        filtered_heights = [];
        return;
    end
    
    % Рассчитываем чистую высоту пиков (над фоном)
    net_heights = spectrum(positions) - background(positions);
    
    % Сортируем пики по их чистой высоте
    [~, idx] = sort(net_heights, 'descend');
    sorted_positions = positions(idx);
    keep_mask = true(size(sorted_positions));
    
    for i = 1:length(sorted_positions)
        if keep_mask(i)
            % Находим все пики, которые слишком близко
            close_peaks = abs(sorted_positions - sorted_positions(i)) <= min_distance;
            close_peaks(i) = false;  % Исключаем текущий пик
            keep_mask(close_peaks) = false;
        end
    end
    
    filtered_positions = sort(sorted_positions(keep_mask));
    filtered_heights = net_heights(keep_mask);
end