/*
 * main_display.c
 *
 *  Created on: Oct 18, 2026
 *      Author: ed
 */

#include "main_display.h"

unsigned char lcd_str_buffer[40] = {0,};
float last_level = 0;
float level_delta = 0;
/* LCD display */
SharpLCD_t lcd;

/* Инициализация LCD дисплея */
void lcd_init(void) {
	/* Запускаем таймер для обновления экрана */
	//UTIL_LPM_SetOffMode(1U << CFG_LPM_APP, UTIL_LPM_DISABLE);
	HAL_TIM_OC_Start(&htim1, TIM_CHANNEL_4);
	//__HAL_TIM_MOE_ENABLE(&htim1); // Принудительно открываем выход Advanced-таймера
	SharpLCD_Init(&lcd, &hspi1, DISPLAY_CS_GPIO_Port, DISPLAY_CS_Pin, DISPLAY_ENABLE_GPIO_Port, DISPLAY_ENABLE_Pin);
	SharpLCD_ClearAll(&lcd);
	SharpLCD_DrawIcon(&lcd, 5, 1, bluetooth_icon_bits);
	SharpLCD_DrawBuffer(&lcd);
}

void redraw_display(void) {
	UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_DISABLE);
	HAL_TIM_OC_DeInit(&htim1);
	MX_TIM1_Init();
	HAL_TIM_OC_Stop(&htim1, TIM_CHANNEL_4);
	HAL_TIM_OC_Start(&htim1, TIM_CHANNEL_4);
	HAL_SPI_DeInit(&hspi1);
	MX_SPI1_Init();

	draw_dosimeter();
/*
	sprintf((char *) lcd_str_buffer, "cnt: %lu", currentTime);
	SharpLCD_WriteString(5, 50, 0, (char *) lcd_str_buffer, 0);

	sprintf((char *) lcd_str_buffer, "bat: %u", currVoltage);
	SharpLCD_WriteString(5, 60, 0, (char *) lcd_str_buffer, 0);
	sprintf((char *) lcd_str_buffer, "tmp: %u", currTemperature);
	SharpLCD_WriteString(5, 70, 0, (char *) lcd_str_buffer, 0);
	//SharpLCD_DrawVLine(114, 3, 6, 0);
	//SharpLCD_DrawVLine(115, 3, 6, 0);
	//SharpLCD_DrawVLine(116, 3, 6, 0);
*/
	/* Уровень батареи */
	SharpLCD_DrawIcon(&lcd, 110, 1, Battary_AC_icon_bits);
	if ((float) currVoltage * battKoeff.Float >= LOW_BATT) {
		float min_edge = LOW_BATT / battKoeff.Float;
		float max_edge = 4096.0f;
		float step_batt = 10 / (float)(max_edge - min_edge);
		uint16_t indicator_bat = round((currVoltage - min_edge) * step_batt);
		if (indicator_bat > 10) {
			indicator_bat = 0;
		}
		SharpLCD_FillRect(114, 3, indicator_bat, 6, 0);
		//sprintf((char *) lcd_str_buffer, "i:%u", indicator_bat);
		//SharpLCD_WriteString(10, 1, 0, (char *) lcd_str_buffer, 0);
	}

	/* Индикатор BLE подключения */
	if (connectFlag) {
		SharpLCD_DrawIcon(&lcd, 5, 1, bluetooth_icon_bits);
	} else {
		SharpLCD_ClearIcon(&lcd, 5, 1, bluetooth_icon_bits);
	}

	draw_activity();
	draw_indicator();

	SharpLCD_DrawBuffer(&lcd);

}

		  /*
		   * Индикатор изменения активности
		   */
void draw_activity(void) {
	  SharpLCD_FillRect(0, ACTIVITY_Y - ACTIVITY_H, SHARP_WIDTH, ACTIVITY_H, 1);
	  SharpLCD_DrawVLine(SHARP_WIDTH / 2, ACTIVITY_Y - ACTIVITY_H, ACTIVITY_H, 0);
	  SharpLCD_DrawHLine(2, ACTIVITY_Y, SHARP_WIDTH - 4, 0);
	  float level_delta;
	  bool level_direct;
	  if (last_level < CPS ) {
		  level_delta = CPS - last_level;
		  level_direct = false;
	  } else {
		  level_delta = last_level - CPS;
		  level_direct = true;
	  }
	  last_level = CPS;
	  level_delta = level_delta * ACTIVITY_SCALE;
	  if (level_delta > SHARP_WIDTH / 2 - 1) {
		  level_delta = SHARP_WIDTH / 2;
	  }
	  //sprintf((char *) lcd_str_buffer, "%.0f    ", level_delta);
	  //SharpLCD_WriteString(0, 10, F5X8, (char *) lcd_str_buffer, 0);
	  if (level_direct) {
		  SharpLCD_FillRect((SHARP_WIDTH / 2) - level_delta + 1, ACTIVITY_Y - ACTIVITY_H, level_delta, ACTIVITY_H - 1, 0);
	  } else {
		  SharpLCD_FillRect(SHARP_WIDTH / 2, ACTIVITY_Y - ACTIVITY_H, level_delta, ACTIVITY_H - 1, 0);
	  }
}

void draw_indicator(void) {
	  /*
	   * Индикатор уровня относительно максимума
	   */
	  SharpLCD_FillRect(0, LEVEL_Y - LEVEL_H, SHARP_WIDTH, LEVEL_H, 1);
	  /* Рисуем только при установленных уровнях */

	  if (level1 > 0) {
		  SharpLCD_DrawVLine(SHARP_WIDTH / 2, LEVEL_Y - LEVEL_H, LEVEL_H, 0);
		  SharpLCD_DrawHLine(2, LEVEL_Y, SHARP_WIDTH - 4, 0);
		  level_delta = (float) SHARP_WIDTH / level1 * CPS;
		  if (level_delta > SHARP_WIDTH - 4) {
			  level_delta = SHARP_WIDTH - 4;
		  }
		  SharpLCD_FillRect(2, LEVEL_Y - LEVEL_H, level_delta, LEVEL_H - 1, 0);
	  }
}

void draw_dosimeter(void) {
	  /* Обновление графика дозиметра */
	  uint16_t idx_dosimeter;
	  uint32_t max_dosimeter;
	  idx_dosimeter = indexDozimetrBufer;
	  max_dosimeter = 0;
	  /* Максимальное значение для масштабирования гистограммы */
	  for (int i = 0; i < SIZE_DOZIMETR_BUFER; i++) {
		  if (dozimetrBuffer[i] > max_dosimeter) {
			  max_dosimeter = dozimetrBuffer[i];
		  }
	  }
	  float resize_koeff = (float) HEIGHT_GISTOGRAM / max_dosimeter;
	  //sprintf((char *) lcd_str_buffer, "Max:%lu", max_dosimeter);
	  //SharpLCD_WriteString(5, 60, 0, (char *) lcd_str_buffer);
	  /* Отрисовка гистограммы */
	  for (int i = 0; i < SHARP_WIDTH; i++) {
		  /* Очистим значение гистограммы */
		  SharpLCD_DrawVLine(i, SHARP_HEIGHT - HEIGHT_GISTOGRAM, HEIGHT_GISTOGRAM, 1);
		  /* Вычислим размер значения гистограммы */
		  uint32_t y_size = dozimetrBuffer[idx_dosimeter++] * resize_koeff;
		  SharpLCD_DrawVLine(i, SHARP_HEIGHT - y_size, y_size, 0);
		  if (idx_dosimeter >= SIZE_DOZIMETR_BUFER) {
			  idx_dosimeter = 0;
		  }
	  }

	  /* Средний CPS по интервалу заданному в DOSIMETER_WIN * DELAY_03 */
	  uint32_t summ_interval;
	  //uint16_t idx_dosimeter;
	  uint16_t cnt_interval;
	  idx_dosimeter = indexDozimetrBufer;
	  summ_interval = 0;
	  cnt_interval = 0;
	  for (uint8_t i = 0; i <= DOSIMETER_WIN; i++) {
		  if (idx_dosimeter == 0) {
			  idx_dosimeter = SIZE_DOZIMETR_BUFER - 1;
		  }
		  if (dozimetrBuffer[idx_dosimeter] > 0) {
			  cnt_interval++;
			  summ_interval += dozimetrBuffer[idx_dosimeter];
		  }
		  idx_dosimeter--;
	  }
	  /* Проверим наличие данных для обработки */
	  if (cnt_interval >= 0) {
		  //cps_find = (float) summ_interval * 1000 / DELAY_03 / cnt_interval;
		  uint16_t x_pos;
		  if (CPS < 10) {
			  x_pos = CPS_X_POSITION + 36;
		  } else if (CPS < 100) {
			  x_pos = CPS_X_POSITION + 18;
		  } else {
			  x_pos = CPS_X_POSITION;
		  }
		  sprintf((char *) lcd_str_buffer, "REAL");
		  SharpLCD_WriteString(3, CPS_Y_POSITION, F5X8, (char *) lcd_str_buffer, 0);
		  /* Проверим на переполнение */
		  if (CPS < 999) {
			  sprintf((char *) lcd_str_buffer, "   ");
			  SharpLCD_WriteString(CPS_X_POSITION, CPS_Y_POSITION, F18X27, (char *) lcd_str_buffer, 0);
			  sprintf((char *) lcd_str_buffer, "%lu   ", CPS);
			  SharpLCD_WriteString(x_pos, CPS_Y_POSITION, F18X27, (char *) lcd_str_buffer, 0);
		  } else {
			  sprintf((char *) lcd_str_buffer, "OVER");
			  SharpLCD_WriteString(CPS_X_POSITION, CPS_Y_POSITION, F18X27, (char *) lcd_str_buffer, 0);
		  }
	  }

	  /* Средний cps по полному времени измерения */
	  //cps_total = (float) dosimeter_pulse_counter * 1000 / meassurment_time;
	  uint16_t x_pos;
	  if (CPS < 10) {
		  x_pos = CPS_X_POSITION + 36;
	  } else if (CPS < 100) {
		  x_pos = CPS_X_POSITION + 18;
	  } else {
		  x_pos = CPS_X_POSITION;
	  }
	  /* Проверим на переполненние */
	  if (CPS < 999) {
		  sprintf((char *) lcd_str_buffer, "   ");
		  SharpLCD_WriteString(CPS_X_POSITION, CPS_Y_POSITION + 28, F18X27, (char *) lcd_str_buffer, 0);
		  sprintf((char *) lcd_str_buffer, "%lu    ", CPS);
		  SharpLCD_WriteString(x_pos, CPS_Y_POSITION + 28, F18X27, (char *) lcd_str_buffer, 0);
	  } else {
		  sprintf((char *) lcd_str_buffer, "OVER");
		  SharpLCD_WriteString(CPS_X_POSITION, CPS_Y_POSITION + 28, F18X27, (char *) lcd_str_buffer, 0);
	  }
	  sprintf((char *) lcd_str_buffer, "AVG");
	  SharpLCD_WriteString(3, CPS_Y_POSITION + 28, F5X8, (char *) lcd_str_buffer, 0);
	  if (pulseCounter > 0) {
		  float accuracy = (float) 900 / pulseCounter;
		  sprintf((char *) lcd_str_buffer, "%.1f%% ", accuracy);
		  SharpLCD_WriteString(3, CPS_Y_POSITION + 38, F5X8, (char *) lcd_str_buffer, 0);
	  }

}
