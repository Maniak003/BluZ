
#include "rw_FLASH.h"

/*
 * rw_FLASH.c
 *
 *  Created on: Jul 8, 2024
 *      Author: ed
 *
 *      Block size 8k
 *      Flash size 1M
 *      Flash start: 0x08000000
 *      Flash end  : 0x080FFFFF
 *
 *
 * typedef enum SNVMA_Cmd_Status
 * SNVMA_ERROR_OK							- 0
 * SNVMA_ERROR_NOK							- 1
 * SNVMA_ERROR_NOT_INIT						- 2
 * SNVMA_ERROR_ALREADY_INIT					- 3
 * SNVMA_ERROR_CMD_PENDING					- 4
 * SNVMA_ERROR_BANK_OP_ONGOING				- 5
 * SNVMA_ERROR_NVM_NULL						- 6
 * SNVMA_ERROR_NVM_NOT_ALIGNED				- 7
 * SNVMA_ERROR_NVM_OVERLAP_FLASH			- 8
 * SNVMA_ERROR_NVM_BUFFER_FULL				- 9
 * SNVMA_ERROR_NVM_BANK_EMPTY				- 10
 * SNVMA_ERROR_NVM_BANK_CORRUPTED			- 11
 * SNVMA_ERROR_CRC_INIT						- 12
 * SNVMA_ERROR_BANK_NUMBER					- 13
 * SNVMA_ERROR_BANK_SIZE					- 14
 * SNVMA_ERROR_BUFFERID_NOT_KNOWN			- 15
 * SNVMA_ERROR_BUFFERID_NOT_REGISTERED		- 16
 * SNVMA_ERROR_BUFFER_NULL					- 17
 * SNVMA_ERROR_BUFFER_NOT_ALIGNED			- 18
 * SNVMA_ERROR_BUFFER_SIZE					- 19
 * SNVMA_ERROR_BUFFER_CONFIG_MISSMATCH		- 20
 * SNVMA_ERROR_FLASH_ERROR					- 21
 * SNVMA_ERROR_UNKNOWN						- 22
 *
 */

uint8_t retryCount;

void CB_SMA (SNVMA_Callback_Status_t os) {
	if (os != SNVMA_OPERATION_COMPLETE) {
		if (retryCount++ < 3) {		// Ограничим количество повторов, что бы не испортить eeprom.
			SNVMA_Write (APP_BLE_NvmBuffer, CB_SMA);
		}
	}

}


/* Запись параметров во flash контроллера */
HAL_StatusTypeDef writeFlash() {
	//FD_FlashOp_Status_t stat_ok = FD_FLASHOP_SUCCESS;
	SNVMA_Cmd_Status_t stat_cmd;
	uint16_t idxPL = 1;
	uint64_t tmpData = 0;
	PL[idxPL++] = (uint64_t) MAGIC_KEY;		// 0, 1
	//PL[idxPL++] = MAGIC_KEY;				// 0, 1

	if (SoundEnable) {
		tmpData = 1;
	} else {
		tmpData = 0;
	}

	if(LEDEnable) {
		tmpData |= 1 << 1;
	}

	if(VibroEnable) {
		tmpData |= 1 << 2;
	}

	if(levelSound1) {
		tmpData |= 1 << 3;
	}

	if(levelSound2) {
		tmpData |= 1 << 4;
	}

	if(levelSound3) {
		tmpData |= 1 << 5;
	}

	if(levelVibro1) {
		tmpData |= 1 << 6;
	}

	if(levelVibro2) {
		tmpData |= 1 << 7;
	}

	if(levelVibro3) {
		tmpData |= 1 << 8;
	}

	switch (resolutionSpecter) {
		case resolution1024: break;
		case resolution2048: {
			tmpData |= 1 << 9;
			break;
		}
		case resolution4096: {
			tmpData |= 1 << 10;
			break;
		}
	}

	if (autoStartSpecrometr) {
		tmpData |= 1 << 11;
	}

	tmpData |= HVoltage << 12;
	tmpData |= (uint64_t)comparatorLevel << 22;

	tmpData |= ((uint64_t)calcCoeff.Uint32 << 32);
	PL[idxPL++] = tmpData;					// 2,  3

	tmpData = level1 | ((uint64_t)level2 << 16) | ((uint64_t)level3 << 32 );
	PL[idxPL++] = tmpData;					// 4,  5

	tmpData = enCoefA1024.Uint32 | ((uint64_t)enCoefB1024.Uint32 << 32);
	PL[idxPL++] = tmpData;					// 6,  7

	tmpData = enCoefC1024.Uint32 | ((uint64_t)enCoefA2048.Uint32 << 32);
	PL[idxPL++] = tmpData;					// 8, 9

	tmpData = enCoefB2048.Uint32 | ((uint64_t)enCoefC2048.Uint32 << 32);
	PL[idxPL++] = tmpData;					// 10, 11

	tmpData = enCoefA4096.Uint32 | ((uint64_t)enCoefB4096.Uint32 << 32);
	PL[idxPL++] = tmpData;					// 12, 13

	tmpData = enCoefC4096.Uint32 | (uint64_t)dozimetrAquracy << 32;
	PL[idxPL++] = tmpData;					// 14, 15

	//PL[idxPL++] = 0xDDDDDDFFCCCCCCFF;		// 16, 17
	//PL[idxPL++] = MAGIC_KEY;		// 20, 21
	retryCount = 0;	// Количество повторов записи
	stat_cmd = SNVMA_Write (APP_BLE_NvmBuffer, CB_SMA);
	if (stat_cmd != SNVMA_ERROR_OK) {
		return HAL_ERROR;
	}


	return HAL_OK;
}

/* Чтение параметров из flash контроллера */
HAL_StatusTypeDef readFlash() {

	uint16_t idxPL = 2;

		/*
		 *  Чтение flash в конфигурационные переменные
		 *
		 *  Адресация во Flash (uint32_t)
		 *  0, 1		--	MAGIC_KEY
		 *
		 *  2			--	Флаги конфигурации
		 *  		0	--  Звуковое сопровождение прихода квантов
		 *  		1	--	Светодиод сопровождает приход квантов
		 *  		2	--	Вибро для служебных событий
		 *  		3	--	Звук для первого уровня
		 *  		4	--	Звук для второго уровня
		 *  		5	--	Звук для третьего уровня
		 *  		6	--	Вибро для первого уровня
		 *  		7	--	Вибро для второго уровня
		 *  		8	--	Вибро для третьего уровня
		 *  		9	--	Разрешение спектра [0:2]
		 *  		10	--	Разрешение спектра [1:2]
		 *  		11	--	Набирать спектр при включении
		 *  		12	--	DAC высокого напряжения [0:10]
		 *  		13	--	DAC высокого напряжения [1:10]
		 *  		14	--	DAC высокого напряжения [2:10]
		 *  		15	--	DAC высокого напряжения [3:10]
		 *  		16	--	DAC высокого напряжения [4:10]
		 *  		17	--	DAC высокого напряжения [5:10]
		 *  		18	--	DAC высокого напряжения [6:10]
		 *  		19	--	DAC высокого напряжения [7:10]
		 *  		20	--	DAC высокого напряжения [8:10]
		 *  		21	--	DAC высокого напряжения [9:10]
		 *  		22	--	DAC уровня компаратора  [0:10]
		 *  		23	--	DAC уровня компаратора  [1:10]
		 *  		24	--	DAC уровня компаратора  [2:10]
		 *  		25	--	DAC уровня компаратора  [3:10]
		 *  		26	--	DAC уровня компаратора  [4:10]
		 *  		27	--	DAC уровня компаратора  [5:10]
		 *  		28	--	DAC уровня компаратора  [6:10]
		 *  		29	--	DAC уровня компаратора  [7:10]
		 *  		30	--	DAC уровня компаратора  [8:10]
		 *  		31	--	DAC уровня компаратора  [9:10]
		 *
		 *	3			--	Коэффициент для пересчета CPS в uRh
		 *	4			--	Уровень первого порога
		 *	5			--	Уровень второго порога
		 *	6			--	Уровень третьего порога
		 *	7			--	N/A
		 *	8			--	Коэффициент A пересчета канала в энергию для 1024
		 *	9			--	Коэффициент B пересчета канала в энергию для 1024
		 *	10			--	Коэффициент C пересчета канала в энергию для 1024
		 *
		 */

		/* Параметры устройства */
		uint64_t tmpData = PL[idxPL++];				// 2, 3
		SoundEnable = tmpData & 1;
		LEDEnable = tmpData & 1 << 1;
		VibroEnable = tmpData & 1 << 2;
		levelSound1 = tmpData & 1 << 3;
		levelSound2 = tmpData & 1 << 4;
		levelSound3 = tmpData & 1 << 5;
		levelVibro1 = tmpData & 1 << 6;
		levelVibro2 = tmpData & 1 << 7;
		levelVibro3 = tmpData & 1 << 8;
		autoStartSpecrometr = tmpData & 1 << 11;
		resolutionSpecter = (tmpData >> 9) & 0x3;	// Разрешение спектра с 9 по 10 разряды
		HVoltage = (tmpData >> 12) & 0x3FF;			// DAC высокое напряжение с 12 по 21 разряды
		comparatorLevel = (tmpData >> 22) & 0x3FF;	// DAC компаратора с 22 по 31 разряды

		/* Коэффицент пересчета cps в uRh */
		calcCoeff.Uint32 = (tmpData >> 32) & 0xFFFFFFFF;

		/* Значения порогов */
		tmpData = PL[idxPL++];						// 4, 5
		level1 = tmpData & 0xFFFF;
		level2 = (tmpData >> 16) & 0xFFFF;
		level3 = (tmpData >> 32) & 0xFFFF;

		/* Коэффициент A полинома преобразования канала в энергию для 1024 каналов*/
		tmpData = PL[idxPL++];						// 6, 7
		enCoefA1024.Uint32 = tmpData & 0xFFFFFFFF;

		/* Коэффициент B полинома преобразования канала в энергию для 1024 каналов */
		enCoefB1024.Uint32 = (tmpData >> 32) & 0xFFFFFFFF;

		/* Коэффициент C полинома преобразования канала в энергию для 1024 каналов */
		tmpData = PL[idxPL++];						// 8, 9
		enCoefC1024.Uint32 = tmpData & 0xFFFFFFFF;

		/* Коэффициент A полинома преобразования канала в энергию для 2048 каналов*/
		enCoefA2048.Uint32 = (tmpData >> 32) & 0xFFFFFFFF;

		/* Коэффициент B полинома преобразования канала в энергию для 2048 каналов */
		tmpData = PL[idxPL++];						// 10, 11
		enCoefB2048.Uint32 = tmpData & 0xFFFFFFFF;

		/* Коэффициент C полинома преобразования канала в энергию для 2048 каналов */
		enCoefC2048.Uint32 = (tmpData >> 32) & 0xFFFFFFFF;

		/* Коэффициент A полинома преобразования канала в энергию для 4096 каналов*/
		//tmpData = *(__IO uint32_t*) ((uint32_t) KOEF_A4096_ADDRESS);
		tmpData = PL[idxPL++];						// 12, 13
		enCoefA4096.Uint32 = tmpData & 0xFFFFFFFF;

		/* Коэффициент B полинома преобразования канала в энергию для 4096 каналов */
		enCoefB4096.Uint32 = (tmpData >> 32) & 0xFFFFFFFF;

		/* Коэффициент C полинома преобразования канала в энергию для 4096 каналов */
		//tmpData = *(__IO uint32_t*) ((uint32_t) KOEF_C4096_ADDRESS);
		tmpData = PL[idxPL++];						// 14, 15
		enCoefC4096.Uint32 = tmpData & 0xFFFFFFFF;

		/* Количество импульсов до усреднения для дозиметра */
		dozimetrAquracy = (tmpData >> 32) & 0xFFFF;
		if ((dozimetrAquracy == 0xFFFF) || (dozimetrAquracy == 0)) {
			dozimetrAquracy = 100;
		}
	return HAL_OK;

}

