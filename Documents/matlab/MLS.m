% Загрузка спектра (пример)
spectrum = importdata('fon.dat'); 

% Параметры сглаживания
lambda = 50;  % Для низкоактивного спектра
d = 2;         % Порядок разности

% Применение MLS
smoothed = poisson_smooth_mls(spectrum, lambda, d);

% Визуализация
subplot(2,1,1)
plot(log(spectrum), 'b')
title('Исходный спектр')

subplot(2,1,2)
plot(log(smoothed), 'r')
title('MLS-сглаживание')
xlabel('Канал')


function smoothed = poisson_smooth_mls(y, lambda, d, max_iter, tol)
% POISSON_SMOOTH_MLS Сглаживание спектров методом максимального правдоподобия
%   y - исходный спектр (вектор-столбец)
%   lambda - параметр сглаживания (10-1000)
%   d - порядок разностей (рекомендуется 2)
%   max_iter - максимум итераций (по умолчанию 50)
%   tol - критерий остановки (по умолчанию 1e-5)

% Проверка входных параметров
if nargin < 3, d = 2; end
if nargin < 4, max_iter = 50; end
if nargin < 5, tol = 1e-5; end

% Инициализация с защитой от нулевых значений
n = length(y);
mu = y + 0.25;  % Псевдосчёт для нулевых каналов

% Построение матрицы регуляризации
E = speye(n);  % Разреженная единичная матрица
D = diff(E, d);  % Матрица разностей d-го порядка
R = lambda * (D' * D);  % Матрица штрафа

% Итеративный алгоритм
for iter = 1:max_iter
    mu_old = mu;
    
    % Веса и рабочий отклик
    w = max(mu, 1e-3);  % Защита от отрицательных значений
    z = log(w) + (y - mu) ./ w;
    
    % Решение системы уравнений
    W = spdiags(w, 0, n, n);  % Разреженная диагональная матрица весов
    A = W + R;
    b = W * z;
    
    % Решение СЛАУ методом сопряженных градиентов
    theta = pcg(A, b, 1e-6, 100);  
    
    % Обновление оценки
    mu = exp(theta);
    
    % Проверка сходимости
    delta = norm(mu - mu_old) / norm(mu);
    if delta < tol
        break;
    end
end

smoothed = mu;
end