function peak_positions = find_peaks_spectrum(spectrum)
    % Параметры обработки (можно адаптировать)
    window_size = 15;           % Размер окна сглаживания (нечетное)
    polynomial_order = 3;        % Порядок полинома
    min_peak_height = 200;        % Минимальная высота пика
    min_2nd_derivative = -1;   % Порог второй производной
    min_peak_distance = 50;      % Минимальное расстояние между пиками
    
    % 1. Сглаживание спектра и вычисление производных
    [~, g] = sgolay(polynomial_order, window_size);
    half_window = (window_size-1)/2;
    
    % Инициализация массивов
    smoothed = zeros(size(spectrum));
    first_deriv = zeros(size(spectrum));
    second_deriv = zeros(size(spectrum));
    
    % Обработка для каждой точки (исключая края)
    for n = (window_size+1)/2 : length(spectrum) - (window_size-1)/2
        % Сглаженное значение
        smoothed(n) = dot(g(:,1), spectrum(n - half_window : n + half_window));
        
        % Первая производная
        first_deriv(n) = dot(g(:,2), spectrum(n - half_window : n + half_window));
        
        % Вторая производная (удвоенная)
        second_deriv(n) = 2 * dot(g(:,3), spectrum(n - half_window : n + half_window));
    end
    
    % 2. Поиск кандидатов в пики
    candidates = [];
    for ch = 2 : (length(spectrum) - 1)
        % Поиск локальных максимумов в сглаженном спектре
        is_local_max = smoothed(ch) > smoothed(ch-1) && ...
                      smoothed(ch) > smoothed(ch+1);
        
        % Проверка порогов
        meets_height = smoothed(ch) > min_peak_height;
        meets_deriv = second_deriv(ch) < min_2nd_derivative;
        
        if is_local_max && meets_height && meets_deriv
            candidates = [candidates; ch];
        end
    end
    
    % 3. Фильтрация близких пиков
    candidates = sort(candidates);
    peak_positions = [];
    
    if ~isempty(candidates)
        last_peak = candidates(1);
        peak_positions = last_peak;
        
        for i = 2:length(candidates)
            if candidates(i) - last_peak >= min_peak_distance
                peak_positions(end+1) = candidates(i);
                last_peak = candidates(i);
            end
        end
    end
    
    % 4. Визуализация результатов
    figure('Position', [100, 100, 900, 600])
    
    % Исходный спектр
    subplot(3,1,1)
    plot(spectrum, 'b')
    hold on
    plot(smoothed, 'r')
    title('Оригинальный и сглаженный спектр')
    xlabel('Канал')
    ylabel('Интенсивность')
    legend('Оригинал', 'Сглаженный')
    grid on
    
    % Производные
    subplot(3,1,2)
    plot(first_deriv, 'g')
    title('Первая производная')
    xlabel('Канал')
    ylabel('dY/dX')
    grid on
    
    subplot(3,1,3)
    plot(second_deriv, 'm')
    hold on
    yline(min_2nd_derivative, '--r', 'Порог')
    title('Вторая производная')
    xlabel('Канал')
    ylabel('d^2Y/dX^2')
    grid on
    
    % Отметить пики на спектре
    subplot(3,1,1)
    plot(peak_positions, smoothed(peak_positions), 'ko', ...
        'MarkerSize', 8, 'LineWidth', 2)
    
    fprintf('Найдено пиков: %d\n', length(peak_positions));
    disp('Позиции пиков (каналы):');
    disp(peak_positions);
end