function scintillator_pulse()
    % Параметры импульса
    A = 3;                  % Амплитуда (В)
    A1 = 1.5;                  % Амплитуда (В)
    rise_time = 800e-9;     % Время до максимума (с)
    fall_time = 2e-6;       % Время спада (с)
    
    % Расчет постоянных времени
    tau_rise = rise_time / 2.5;  % Эмпирический коэффициент для времени нарастания
    tau_fall = fall_time;        % Постоянная времени спада
    
    % Временная ось (0-10 мкс с шагом 1 нс)
    t = -10e-7:1e-9:10e-6;
    
    % Классическая двуэкспоненциальная форма
    V = A * (exp(-t/tau_fall) - exp(-t/tau_rise));
    V1 = A1 * (exp(-t/tau_fall) - exp(-t/tau_rise));
    
    % Нормировка на максимум
    V = V * (A / max(V));
    
    % Находим реальное время максимума
    [Vmax, idx] = max(V);
    t_max = t(idx);
    
    % Визуализация
    figure;
    plot(t*1e6, V, 'b', 'LineWidth', 1);
    hold on;
    plot(t*1e6, V1, 'c', 'LineWidth', 1);
    grid off;
    hold on;
    
    % Отметка времени максимума
    l_max = plot(t_max*1e6, Vmax, 'ro', 'MarkerSize', 6, 'LineWidth', 1);
    l_lev = plot(0, 0.01*Vmax, 'go', 'MarkerSize', 6, 'LineWidth', 1);
    l_end = plot(t_max*1e6, 0.01*Vmax, 'mo', 'MarkerSize', 6, 'LineWidth', 1);
    
    % Подписи
    title('Сигнал сцинтиллятора', 'FontSize', 14);
    legend([l_max, l_lev, l_end], {'Максимум', 'Компаратор', 'Сигнал в УВХ'}, 'Location', 'NorthEast');
    legend('boxoff');
    
    % Текстовые аннотации
    text(0.4, 0.6, {'Время между срабатыванием компаратора и окончанием работы УВХ - фиксированное.', 'Состоит из:', 'Задержки компаратора.', 'Времени запуска ADC.', 'Времени работы схемы захвата.'}, 'Units', 'normalized', 'FontSize', 10, 'BackgroundColor', 'white');
    %text(0.6, 0.6, sprintf('τ_{fall} = %.2f мкс', tau_fall*1e6), ...
    %     'Units', 'normalized', 'FontSize', 10, 'BackgroundColor', 'white');
    
    % Уровень компаратора
    plot([0 t_max*1e6], [0.01*Vmax 0.01*Vmax], 'k--', 'HandleVisibility', 'off');
    plot([t_max*1e6 t_max*1e6], [0 Vmax], 'k--', 'HandleVisibility', 'off');
    
    % Настройка осей
    xlim([-0.5 10]);
    ylim([0 A*1.01]);
    set(gca, 'FontSize', 10);
    box on;
end