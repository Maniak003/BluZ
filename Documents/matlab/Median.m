% Загрузка данных
spectrum = importdata('fon.dat'); 

med = medianFilter(spectrum, 3);
%med = sma2(spectrum, 3);


% Визуализация
subplot(2,1,1)
plot(spectrum, 'b')
title('Исходный спектр')

subplot(2,1,2)
plot(med, 'r')
title('MED-сглаживание')
xlabel('Канал')
