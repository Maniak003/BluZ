function smoothedSpectrum = sma2(gammaSpectrum, windowSize)
    % Проверка на корректный ввод окна
    if mod(windowSize, 2) == 0
        error('Размер окна должен быть нечётным');
    end
    
    % Определение размера спектра и инициализация сглаженного спектра
    spectrumLength = length(gammaSpectrum);
    smoothedSpectrum = zeros(1, spectrumLength);
    halfWindowSize = floor(windowSize / 2);
    
    % Сглаживание спектра
    for i = 1:spectrumLength
        % Определяем индексы для окна
        startIdx = max(1, i - halfWindowSize);
        endIdx = min(spectrumLength, i + halfWindowSize);
        
        % Извлечение подмассива для вычисления среднего
        windowValues = gammaSpectrum(startIdx:endIdx);
        
        % Считаем среднее значение
        smoothedSpectrum(i) = mean(windowValues);
    end
end