#ifndef SHARP_LCD_H
#define SHARP_LCD_H

#include "stm32wbaxx_hal.h"
#include <stdint.h>
#include <string.h>
#include "font_5x8.h"
#include "Arial_18x27.h"

/*
 * DH05
 *
 */
#define LCDTYPE 0

#if (LCDTYPE == 1)
  #define SHARP_WIDTH         144
  #define SHARP_HEIGHT        168
#else
  #define SHARP_WIDTH         128
  #define SHARP_HEIGHT        128
#endif
#define SHARP_BYTES_LINE    (SHARP_WIDTH / 8)
#define SHARP_BUFFER_SIZE   (SHARP_HEIGHT * SHARP_BYTES_LINE)

#define SHARP_CMD_WRITE     0x80
#define SHARP_CMD_CLEAR     0x20
#define SHARP_TRAILER       0x00

typedef struct {
    SPI_HandleTypeDef *hspi;
    GPIO_TypeDef *cs_port;
    uint16_t cs_pin;
    GPIO_TypeDef *disp_port;
    uint16_t disp_pin;
} SharpLCD_t;

enum {
	F5X8,
	F18X27,
};

enum {
	BLACK_COL,
	WHITE_COL
};

extern uint8_t tx_buffer[1 + SHARP_HEIGHT * (1 + SHARP_BYTES_LINE) + 1];

void SharpLCD_SetStatic(SharpLCD_t *lcd);

void SharpLCD_DrawBuffer(SharpLCD_t *lcd);

void SharpLCD_Init(SharpLCD_t *lcd, SPI_HandleTypeDef *hspi,
                   GPIO_TypeDef *cs_port, uint16_t cs_pin,
                   GPIO_TypeDef *disp_port, uint16_t disp_pin);

void SharpLCD_Disable(SharpLCD_t *lcd);

//void SharpLCD_Enable(SharpLCD_t *lcd, uint8_t enable);
void SharpLCD_Enable(SharpLCD_t *lcd, uint8_t enable);

void SharpLCD_Select(SharpLCD_t *lcd, uint8_t enable);

void SharpLCD_DrawPixel(int16_t x, int16_t y, uint8_t color);

void SharpLCD_FillRect(int16_t x, int16_t y, int16_t w, int16_t h, uint8_t color);

void SharpLCD_DrawLine(SharpLCD_t *lcd, uint8_t line, uint8_t color);

void SharpLCD_ClearAll(SharpLCD_t *lcd);
void SharpLCD_WriteChar(int16_t x, int16_t y, uint8_t font_id, uint8_t chr, uint8_t invert);
void SharpLCD_WriteString(int16_t x, int16_t y, uint8_t font_id, char * str, uint8_t invert);
void SharpLCD_DrawVLine(int16_t x, int16_t y, int16_t height, uint8_t color);
void SharpLCD_DrawHLine(int16_t x, int16_t y, int16_t widht, uint8_t color);
void SharpLCD_DrawIcon(SharpLCD_t *lcd, int16_t x, int16_t y, uint8_t * icon);
void SharpLCD_ClearIcon(SharpLCD_t *lcd, int16_t x, int16_t y, uint8_t * icon);

#endif
