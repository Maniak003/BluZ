function filteredImage = medianFilter(inputImage, windowSize)
    % Проверка корректности размера окна
    if mod(windowSize, 2) == 0
        error('Размер окна должен быть нечётным');
    end

    % Определение размеров входного изображения
    [rows, cols] = size(inputImage);
    
    % Инициализация выходного изображения
    filteredImage = zeros(rows, cols);
    
    % Параметры смещения для окна
    halfWindowSize = floor(windowSize / 2);
    
    % Применение медианного фильтра
    for r = 1:rows
        for c = 1:cols
            % Определение границ окна
            rowStart = max(1, r - halfWindowSize);
            rowEnd = min(rows, r + halfWindowSize);
            colStart = max(1, c - halfWindowSize);
            colEnd = min(cols, c + halfWindowSize);

            % Извлечение подмассива
            window = inputImage(rowStart:rowEnd, colStart:colEnd);
            
            % Рассчет медианы
            filteredImage(r, c) = median(window(:));
        end
    end
    
    % Приведение выходного изображения к исходному типу данных
    filteredImage = cast(filteredImage, class(inputImage));
end