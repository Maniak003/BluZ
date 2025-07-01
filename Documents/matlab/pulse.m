plot_signals_with_max_markers();

function plot_signals_with_max_markers()
    % Параметры сигналов
    A1 = 3;     % Амплитуда первого сигнала
    A2 = 1.5;   % Амплитуда второго сигнала
    A3 = 0.5;   % Амплитуда третьего сигнала

    rise_time = 800e-9;
    fall_time = 2e-6;
    
    % Расчет постоянных времени
    tau_rise = rise_time / 2.5;
    tau_fall = fall_time;
    
    % Временная ось
    t = -10e-7:1e-9:10e-6;
    
    % Базовый сигнал (нормированный на 1)
    V_base = exp(-t/tau_fall) - exp(-t/tau_rise);
    V_base = V_base / max(V_base);
    
    % Создаем два сигнала с разными амплитудами
    V1 = A1 * V_base;
    V2 = A2 * V_base;
    V3 = A3 * V_base;

    % Находим максимумы
    [max1, idx1] = max(V1);
    t_max1 = t(idx1);
    [max2, idx2] = max(V2);
    t_max2 = t(idx2);
    [max3, idx3] = max(V3);
    t_max3 = t(idx3);

    %% Основной график с сигналами
    figure;
    main_ax = axes; % Сохраняем handle основной оси
    
    % Сигнал 1
    h1 = plot(t*1e6, V1, 'b', 'LineWidth', 1);
    hold on;
    
    % Сигнал 2
    h2 = plot(t*1e6, V2, 'r', 'LineWidth', 1);
    hold on;

    % Сигнал 3
    h3 = plot(t*1e6, V3, 'c', 'LineWidth', 1);
    hold on;

    % Настройка основной оси
    %grid on;
    title('Сигналы сцинтиллятора');
    xlabel('Время (мкс)');
    ylabel('Амплитуда (В)');
    xlim([-0.5 10]);
    ylim([0 max1*1.1]);
    set(main_ax, 'FontSize', 10);
    box on;
    
    % Точки максимумов на основном графике
    plot(t_max1*1e6, max1, 'bo', 'MarkerSize', 6, 'MarkerFaceColor', 'b');
    plot(t_max2*1e6, max2, 'ro', 'MarkerSize', 6, 'MarkerFaceColor', 'r');
    plot(t_max2*1e6, max3, 'co', 'MarkerSize', 6, 'MarkerFaceColor', 'c');

    % Компаратор
    l_lev = plot(0, 0.01*max1, 'go', 'MarkerSize', 6, 'MarkerFaceColor', 'g');
    plot([-0.3 2], [0.01*max1 0.01*max1], 'g--', 'HandleVisibility', 'off');

    % Положение максимума, окончание работы УВХ
    line_max = plot([t_max1*1e6 t_max1*1e6], [0 max1*1.1], 'k--');

    % Легенда для основных сигналов
    legend([h1, h2, h3, l_lev, line_max], ...
        {sprintf('Импульс 1 (%.1f В)', A1), ...
         sprintf('Импульс 2 (%.1f В)', A2), ...
         sprintf('Импульс 3 (%.1f В)', A3), ...
         'Уровень компаратора', ...
         'Окончание работы УВХ'}, ...
        'Location', 'best');
    
    % Текстовые аннотации
    text(0.4, 0.8, {'Время между срабатыванием компаратора и окончанием работы УВХ - фиксированное.', 'Состоит из:', 'Задержки компаратора.', 'Времени запуска ADC.', 'Времени работы схемы захвата.'}, 'Units', 'normalized', 'FontSize', 10, 'BackgroundColor', 'white');

    %% Создаем вставку для отображения положения максимумов
    % Позиция вставки: [left, bottom, width, height] в нормированных единицах
    inset_pos = [0.50, 0.30, 0.20, 0.15];
    inset_ax = axes('Position', inset_pos);
    
    % Создаем данные для вставки: время максимумов по X, 1 по Y
    amplitudes = [0, 3.5]; % В мкс
    fix_values = [1, 1]; % Фиксированное значение Y=1
    
    % Рисуем горизонтальную линию на Y=1
    line(amplitudes, fix_values, 'Color', 'k', 'LineWidth', 0.5);
    hold on;
    
    % Рисуем маркеры для максимумов
    h_inset1 = plot(A1, 1, 'bs', 'MarkerSize', 8, 'MarkerFaceColor', 'b');
    h_inset2 = plot(A2, 1, 'rs', 'MarkerSize', 8, 'MarkerFaceColor', 'r');
    h_inset3 = plot(A3, 1, 'cs', 'MarkerSize', 8, 'MarkerFaceColor', 'c');

    % Настройка вставки
    xlim(inset_ax, amplitudes); % Та же шкала времени, что и на основной оси
    ylim(inset_ax, [0.95 1.05]); % Узкий диапазон вокруг Y=1
    grid on;
    title('Спектр', 'FontSize', 9);
    xlabel('Амплитуда (В)', 'FontSize', 8);
    set(inset_ax, 'YTick', [1], 'YTickLabel', {'CNT'});
    set(inset_ax, 'FontSize', 8);
    box on;
    
    %% Добавляем текстовые метки на основной график
    %text(t_max1*1e6, max1*0.8, sprintf('t=%.2f μs', t_max1*1e6), ...
    %    'HorizontalAlignment', 'center', 'FontSize', 9, 'BackgroundColor', 'white');
    
    %text(t_max2*1e6, max2*0.8, sprintf('t=%.2f μs', t_max2*1e6), ...
    %    'HorizontalAlignment', 'center', 'FontSize', 9, 'BackgroundColor', 'white');
    
    %% Добавляем соединительные линии
    % Координаты для соединительных линий (в нормированных единицах фигуры)
    
    % Для первого максимума
    [x1_main, y1_main] = data2norm(main_ax, t_max1*1e6, max1);
    [x1_inset, y1_inset] = data2norm(inset_ax, A1, 1);
    annotation('line', [x1_inset x1_main], [y1_inset y1_main], 'LineStyle', '--', 'Color', 'b');
    
    % Для второго максимума
    [x2_main, y2_main] = data2norm(main_ax, t_max2*1e6, max2);
    [x2_inset, y2_inset] = data2norm(inset_ax, A2, 1);
    annotation('line', [x2_inset x2_main], [y2_inset y2_main], 'LineStyle', '--', 'Color', 'r');

    % Для второго максимума
    [x3_main, y3_main] = data2norm(main_ax, t_max3*1e6, max3);
    [x3_inset, y3_inset] = data2norm(inset_ax, A3, 1);
    annotation('line', [x3_inset x3_main], [y3_inset y3_main], 'LineStyle', '--', 'Color', 'c');

end

%% Вспомогательная функция для преобразования координат данных в нормированные
function [x_norm, y_norm] = data2norm(ax, x_data, y_data)
    % Получаем пределы осей
    xlim = get(ax, 'XLim');
    ylim = get(ax, 'YLim');
    
    % Преобразуем координаты данных в нормированные относительно осей
    x_norm = (x_data - xlim(1)) / (xlim(2) - xlim(1));
    y_norm = (y_data - ylim(1)) / (ylim(2) - ylim(1));
    
    % Получаем позицию оси в нормированных единицах фигуры
    ax_pos = get(ax, 'Position');
    
    % Преобразуем в нормированные координаты фигуры
    x_norm = ax_pos(1) + x_norm * ax_pos(3);
    y_norm = ax_pos(2) + y_norm * ax_pos(4);
end