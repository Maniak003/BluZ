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
#include "rw_FLASH.h"
// FM_Cmd_Status_t FM_Write(uint32_t *Src, uint32_t *Dest, int32_t Size, FM_CallbackNode_t *CallbackNode);
// FM_Cmd_Status_t FM_Erase(uint32_t FirstSect, uint32_t NbrSect, FM_CallbackNode_t *CallbackNode);
// FD_FlashOp_Status_t FD_EraseSectors(uint32_t Sect);
// void FD_SetStatus(FD_Flash_ctrl_bm_t Flags_bm, FD_FLASH_Status_t Status);
// FD_FlashOp_Status_t FD_WriteData(uint32_t Dest, uint32_t Payload);

/* Запись параметров во flash контроллера */
HAL_StatusTypeDef writeFlash() {
	HAL_StatusTypeDef stat_ok = HAL_ERROR;
	while(stat_ok != HAL_OK) {
		stat_ok = HAL_FLASH_Unlock();
	}
	bzero((char *) uartBuffer, sizeof(uartBuffer));
	sprintf(uartBuffer, "\n\rUnlock Ok\n\r");
	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

	stat_ok = HAL_FLASH_Program(FLASH_TYPEPROGRAM_QUADWORD, START_FLASH_ADDRESS, MAGIC_KEY);
	bzero((char *) uartBuffer, sizeof(uartBuffer));
	sprintf(uartBuffer, "\n\rWrite status: %d\n\r", stat_ok);
	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	/*
	FD_FlashOp_Status_t ss;
	FD_SetStatus(FD_FLASHACCESS_SYSTEM | FD_FLASHACCESS_RFTS, LL_FLASH_ENABLE);
	ss = FD_EraseSectors(127);
	bzero((char *) uartBuffer, sizeof(uartBuffer));
	sprintf(uartBuffer, "\n\rErase sector status: %d\n\r", ss);
	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	if (ss == FD_FLASHOP_SUCCESS) {
		ss = FD_WriteData(START_FLASH_ADDRESS, MAGIC_KEY);
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		sprintf(uartBuffer, "\n\rWrite status: %d\n\r", ss);
		HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	}
	FD_SetStatus(FD_FLASHACCESS_SYSTEM | FD_FLASHACCESS_RFTS,  LL_FLASH_DISABLE);
	*/
	while(stat_ok != HAL_OK) {
		stat_ok = HAL_FLASH_Lock();
	}

	return stat_ok;
}

/* Чтение параметров из flash контроллера */
HAL_StatusTypeDef readFlash() {
	/* Flash уже инициализирована */
	if (*(__IO uint32_t*) MAGIC_KEY_ADDRESS == MAGIC_KEY) {

	} else { // Ключ не найден, нужно инициализировать flash
		//if(writeFlash() == HAL_OK) {	}
	}
	return HAL_OK;
}
