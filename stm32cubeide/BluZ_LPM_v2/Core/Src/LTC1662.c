/*
 *	Управление DAC.
 *	port:
 *	CHANNEL_A -- Компаратор
 *	CHANNEL_B -- Высокое напряжение
 *
 *	level:
 *	0 - 0x3FF
 */

#include <stdint.h>
#include "LTC1662.h"

uint16_t currentLevelPortA = 0;
uint16_t currentLevelPortB = 0;
void setLevelOnPort(LTC1662_channel port, uint16_t level) {

	/* Подготовим данные для загрузки в DAC */
	uint16_t loadData = (level << 2) & 0x0FFC ;
	if (port == CHANNEL_A) { 				// Порт А с выводом напряжения
		if (currentLevelPortA != level) {
			currentLevelPortA = level;
			loadData = loadData | 0x9000;
		} else {
			return;							// Если нет изменений - выходим
		}
	} else  if (port == CHANNEL_B) { 		// Порт B с выводом напряжения
		if (currentLevelPortB != level) {
			currentLevelPortB = level;
			loadData = loadData | 0xA000;
		} else {
			return;							// Если нет изменений - выходим
		}
	} else {
		return;								// Указан неправильный канал - выходим
	}

	/* Последовательность для начала изменения уровня */
	HAL_GPIO_WritePin(A_CS_GPIO_Port, A_CS_Pin, GPIO_PIN_RESET);			/* Низкий уровень CS для включения DAC */
	HAL_GPIO_WritePin(A_CS_GPIO_Port, A_CS_Pin, GPIO_PIN_SET);				/* Высокий уровени CS */
	HAL_GPIO_WritePin(A_SCK_GPIO_Port, A_SCK_Pin, GPIO_PIN_SET);			/* Высокий уровень SCK */
	HAL_GPIO_WritePin(A_SCK_GPIO_Port, A_SCK_Pin, GPIO_PIN_RESET);			/* Отключаем SCK */
	HAL_GPIO_WritePin(A_CS_GPIO_Port, A_CS_Pin, GPIO_PIN_RESET);			/* Низкий уровень CS для включения DAC */

	for (int i = 0; i < 16; i++) {
	  if ((loadData & (1 << (15 - i))) == 0) {								/* Сканируем по разрядам */
		  HAL_GPIO_WritePin(A_DATA_GPIO_Port, A_DATA_Pin, GPIO_PIN_RESET);	/* Если 0 */
	  } else {
		  HAL_GPIO_WritePin(A_DATA_GPIO_Port, A_DATA_Pin, GPIO_PIN_SET);	/* Если 0 */
	  }
	  HAL_GPIO_WritePin(A_SCK_GPIO_Port, A_SCK_Pin, GPIO_PIN_SET);			/* Формируем импульс на SCK */
	  HAL_GPIO_WritePin(A_SCK_GPIO_Port, A_SCK_Pin, GPIO_PIN_RESET);
	}
	HAL_GPIO_WritePin(A_CS_GPIO_Port, A_CS_Pin, GPIO_PIN_SET);				/* Устанавливаем CS для выполнения команды */
}
