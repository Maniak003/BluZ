function pu()
    % Параметры сигнала
    A = 3;               % Амплитуда импульса (В)
    T_pulse = 500e-9;    % Длительность импульса (с)
    rise_time = 800e-9;  % Время нарастания до максимума (с)
    fall_time = 2e-6;    % Время спада (с)
    
    % Расчет постоянных времени
    tau_rise = rise_time / 2.2;   % Постоянная времени нарастания
    tau_fall = fall_time / 2.2;   % Постоянная времени спада
    
    % Временной диапазон (0 до 10 мкс с шагом 1 нс)
    t = 0:1e-9:10e-6;
    
    % Инициализация выходного напряжения
    V_out = zeros(size(t));
    
    % Фаза нарастания (0 ≤ t < T_pulse)
    idx_rise = t < T_pulse;
    t_rise = t(idx_rise);
    V_out(idx_rise) = A * (1 - exp(-t_rise / tau_rise));
    
    % Значение в конце импульса
    V_max = A * (1 - exp(-T_pulse / tau_rise));
    
    % Фаза спада (t ≥ T_pulse)
    idx_fall = t >= T_pulse;
    t_fall = t(idx_fall);
    V_out(idx_fall) = V_max * exp(-(t_fall - T_pulse) / tau_fall);
    
    % Построение графика
    figure;
    plot(t * 1e6, V_out, 'LineWidth', 1.5);
    grid on;
    xlabel('Время (мкс)', 'FontSize', 12);
    ylabel('Напряжение (В)', 'FontSize', 12);
    title('Результат интегрирования прямоугольного импульса', 'FontSize', 14);
    
    % Установка пределов оси X
    xlim([0 10]);
    
    % Отметки ключевых точек
    hold on;
    plot(T_pulse * 1e6, V_max, 'ro', 'MarkerSize', 8);
    line([0 max(t)*1e6], [V_max V_max], 'Color', 'k', 'LineStyle', '--');
    text(0.5, V_max+0.1, sprintf('Максимум: %.2f В', V_max), 'FontSize', 10);
    text(T_pulse*1e6 + 0.1, V_max, sprintf('t = %.1f мкс', T_pulse*1e6), 'FontSize', 10);
    hold off;
end