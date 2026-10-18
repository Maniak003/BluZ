#include "sharp_lcd.h"
#include "cmsis_gcc.h"

uint8_t tx_buffer[1 + SHARP_HEIGHT * (1 + SHARP_BYTES_LINE) + 1];

//#include <arm_cmse.h>  // или <cmsis_armcc.h> / <cmsis_gcc.h>

static inline uint8_t reverse_bits(uint8_t b) {
    // __RBIT работает с 32 битами, поэтому сдвигаем результат
    return (uint8_t)(__RBIT((uint32_t)b) >> 24);
}

void SharpLCD_Init(SharpLCD_t *lcd, SPI_HandleTypeDef *hspi,
                   GPIO_TypeDef *cs_port, uint16_t cs_pin,
                   GPIO_TypeDef *disp_port, uint16_t disp_pin) {
    lcd->hspi = hspi;
    lcd->cs_port = cs_port;
    lcd->cs_pin = cs_pin;
    lcd->disp_port = disp_port;
    lcd->disp_pin = disp_pin;

	SharpLCD_Enable(lcd, 1);
	SharpLCD_Select(lcd, 0);
}

void SharpLCD_Disable(SharpLCD_t *lcd) {
	HAL_SPI_DeInit(lcd->hspi);
	SharpLCD_Enable(lcd, 1);
	SharpLCD_Enable(lcd, 0);
	SharpLCD_Select(lcd, 0);
}

void SharpLCD_Select(SharpLCD_t *lcd, uint8_t enable) {
	HAL_GPIO_WritePin(lcd->cs_port, lcd->cs_pin, enable ? GPIO_PIN_SET : GPIO_PIN_RESET);
}

void SharpLCD_Enable(SharpLCD_t *lcd, uint8_t enable) {
    HAL_GPIO_WritePin(lcd->disp_port, lcd->disp_pin, enable ? GPIO_PIN_SET : GPIO_PIN_RESET);
}

void SharpLCD_SetStatic(SharpLCD_t *lcd) {
	uint8_t cmd[2] = {SHARP_TRAILER, SHARP_TRAILER};
	SharpLCD_Select(lcd, 1);
	HAL_Delay(1);
	HAL_SPI_Transmit(lcd->hspi,  cmd, sizeof(cmd), 100);
	HAL_Delay(1);
	SharpLCD_Select(lcd, 0);
}

/* Рисование иконки в заданные координаты */
void SharpLCD_DrawIcon(SharpLCD_t *lcd, int16_t x, int16_t y, uint8_t * icon) {
	uint16_t line_idx = 0;								// Смещение в массиве с учетом двух заголовочный байтов.
	uint8_t witgh_icon = icon[line_idx++];				// Ширина иконки
	uint8_t hight_icon = icon[line_idx++];   			// Высота иконки
	uint8_t line_icon;
	uint8_t b_e = 0;									// Для контроля граници байта
	line_icon = icon[line_idx++];						// Выбираем элемент строки
	for (int i = 0; i < hight_icon; i++) {				// Цикл по линиям иконки
		for (int j = 0; j < witgh_icon; j++) {			// Цикл по пикселям иконки
			if ((line_icon & (1 << b_e++)) == 0) {
				SharpLCD_DrawPixel(x + j, y + i, 1);	// Черная точка  - нет изображения
			} else {
				SharpLCD_DrawPixel(x + j, y + i, 0);	// Белая точка - есть изображение.
			}
			if (b_e > 7) {								// Байт отрисовали
				line_icon = icon[line_idx++];			// Берем следующий байт глифа
				b_e = 0;
			}
		}
	}
}

void SharpLCD_ClearIcon(SharpLCD_t *lcd, int16_t x, int16_t y, uint8_t * icon) {
	uint16_t line_idx = 0;								// Смещение в массиве с учетом двух заголовочный байтов.
	uint8_t witgh_icon = icon[line_idx++];				// Ширина иконки
	uint8_t hight_icon = icon[line_idx++];   			// Высота иконки
	for (int i = 0; i < hight_icon; i++) {				// Цикл по линиям иконки
		for (int j = 0; j < witgh_icon; j++) {			// Цикл по пикселям иконки
			SharpLCD_DrawPixel(x + j, y + i, 1);	// Черная точка - нет изображения
		}
	}
}

/* Обновления содержимого  */
void SharpLCD_DrawBuffer(SharpLCD_t *lcd) {
	SharpLCD_Select(lcd, 1);
	HAL_Delay(1);
	uint8_t line_buff[SHARP_WIDTH / 8 + 4] = {0,};
	uint8_t ln = 1;															// Линия стартовая
	line_buff[0] = SHARP_CMD_WRITE;											// Команда для перевода в динамический режим для записи
	HAL_SPI_Transmit(lcd->hspi,  line_buff, 1, 100);						// Передадим команду
	for (int i = 0; i < SHARP_HEIGHT; i++) {								// Цикл по строкам
		line_buff[0] = reverse_bits(ln++);									// Подготовим номер линии для передачи
		for (int j = 0; j < SHARP_WIDTH / 8; j++) {							// Цикл по столбцам
			line_buff[j + 1] = tx_buffer[i * SHARP_WIDTH / 8 + j];			// Подготовим буфер для передачи линии
		}
		line_buff[SHARP_WIDTH / 8 + 1] = 0x00;								// Требуется 8 тактов в конце каждой линии
		HAL_SPI_Transmit(lcd->hspi, line_buff, SHARP_WIDTH / 8 + 2, 100);	// Передадим линию.
	}
	line_buff[0] = 0x00;
	HAL_SPI_Transmit(lcd->hspi, line_buff, 1, 100);							// Передадим завершающие 8 пустых тактов.
	HAL_Delay(1);
	SharpLCD_Select(lcd, 0);
}

void SharpLCD_DrawLine(SharpLCD_t *lcd, uint8_t line, uint8_t color) {
	uint8_t rline;
	rline = reverse_bits(line);
	uint8_t lin[] = {0x80, rline, color, color, color, color, color, color, color, color, color, color, color, color, color, color, color, color, 0x00, 0x00};
	SharpLCD_Select(lcd, 1);
	HAL_Delay(1);
	HAL_SPI_Transmit(lcd->hspi,  lin, sizeof(lin), 100);
	HAL_Delay(1);
	SharpLCD_Select(lcd, 0);
}

void SharpLCD_ClearAll(SharpLCD_t *lcd) {
	uint8_t cmd[] = {SHARP_CMD_CLEAR, SHARP_TRAILER};
	SharpLCD_Select(lcd, 1);
	HAL_Delay(1);
	HAL_SPI_Transmit(lcd->hspi,  cmd, sizeof(cmd), 100);
	HAL_Delay(1);
	SharpLCD_Select(lcd, 0);
	/* Очистим буфер для дисплея */
	for (int i = 0; i < SHARP_WIDTH / 8 * SHARP_HEIGHT; i++) {
		  tx_buffer[i] = 0xFF;
	}
}

/* Горизонтальная линия */
void SharpLCD_DrawHLine(int16_t x, int16_t y, int16_t widht, uint8_t color) {
	for (int16_t i = 0; i < widht; i++ ) {
		SharpLCD_DrawPixel(x + i, y, color);
	}
}

/* Вертикальная линия */
void SharpLCD_DrawVLine(int16_t x, int16_t y, int16_t height, uint8_t color) {
	for (int16_t i = 0; i < height; i++ ) {
		SharpLCD_DrawPixel(x, y + i, color);
	}
}

/* Вывод строки текста */
void SharpLCD_WriteString(int16_t x, int16_t y, uint8_t font_id, char * str, uint8_t invert) {
	uint8_t ch_with;
	switch (font_id) {
	case F5X8:
		ch_with = 7;
		for (int i = 0; i < strlen(str); i++) {
			SharpLCD_WriteChar(x + i * ch_with, y, font_id, str[i], invert);
		}
		break;
	case F18X27:
		ch_with = 18;
		for (int i = 0; i < strlen(str); i++) {
			SharpLCD_WriteChar(x + i * ch_with, y, font_id, str[i], invert);
		}
		break;
	default:
		break;
	}
}

/* Прорисовка символа с очисткой фона */
void SharpLCD_WriteChar(int16_t x, int16_t y, uint8_t font_id, uint8_t chr, uint8_t invert) {
	if (chr >= 32) {											// Фонт содержит только печатные символы
		uint16_t fnt_addr = chr - 32;
		switch (font_id) {
		/* Шрифт 5x8 */
		case F5X8:
			for (int j = 0; j < 5; j++) {						// Выбираем байт глифа символа
				uint8_t chr_part = font_5x8[fnt_addr][j];		// Выбираем из массива байт глифа
				for (int i = 0; i < 8; i++) {					// Определяем цвет точки в глифе
					if (chr_part & (1 << i)) {
						if (invert == 0) {
						SharpLCD_DrawPixel(x + j, y + i, 0);	// Прорисовываем черную точку
						} else {
							SharpLCD_DrawPixel(x + j, y + i, 1);
						}
					} else {
						if (invert == 0) {
						SharpLCD_DrawPixel(x + j, y + i, 1);	// Прорисовываем белую точку
						} else {
							SharpLCD_DrawPixel(x + j, y + i, 0);
						}
					}
				}
			}
			break;
		/* Шрифт 18x27 */
		case F18X27:
			for (uint8_t j = 0; j < 27; j++) {
				uint32_t chr_part = Arial_18x27[(fnt_addr * 27) + j];	// Выбираем uint32_t для формирования изображения строки
				for (uint8_t i = 0; i < 32; i++) {				// Будем рисовать 32 точки
					if (chr_part & (0x80000000UL >> i)) {
						SharpLCD_DrawPixel(x + i, y + j, 0);
					} else {
						SharpLCD_DrawPixel(x + i, y + j, 1);
					}
				}
			}
			break;
		default:
			break;
		}
	}
}

/*
 *	x - Положение по горизонтали от 0 до SHARP_WIDTH
 *	y - Положение по вертикали от 0 до SHARP_HEIGHT
 *  color - Цвет пиксела. 0 - очистка - нет изображения. 1 - черная точка - есть изображение.
 */
void SharpLCD_DrawPixel(int16_t x, int16_t y, uint8_t color) {
    if (x < 0 || x >= SHARP_WIDTH || y < 0 || y >= SHARP_HEIGHT) return;
    uint16_t idx = y * SHARP_BYTES_LINE + (x / 8);
    uint8_t bit = 7 - (x % 8);
    if (color) tx_buffer[idx] |= (1 << bit);
    else tx_buffer[idx] &= ~(1 << bit);
}

void SharpLCD_FillRect(int16_t x, int16_t y, int16_t w, int16_t h, uint8_t color) {
    for (int16_t j = y; j < y + h; j++) {
        for (int16_t i = x; i < x + w; i++) {
            SharpLCD_DrawPixel(i, j, color);
        }
    }
}
