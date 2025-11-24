/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    BluZ_app.c
  * @author  MCD Application Team
  * @brief   BluZ_app application definition.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "app_common.h"
#include "log_module.h"
#include "app_ble.h"
#include "ll_sys_if.h"
#include "dbg_trace.h"
#include "ble.h"
#include "bluz_app.h"
#include "bluz.h"
#include "stm32_rtos.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <stm32_lpm.h>
#include <ble_raw_api.h>
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

typedef enum
{
  Rx_NOTIFICATION_OFF,
  Rx_NOTIFICATION_ON,
  Tx_NOTIFICATION_OFF,
  Tx_NOTIFICATION_ON,
  /* USER CODE BEGIN Service1_APP_SendInformation_t */

  /* USER CODE END Service1_APP_SendInformation_t */
  BLUZ_APP_SENDINFORMATION_LAST
} BLUZ_APP_SendInformation_t;

typedef struct
{
  BLUZ_APP_SendInformation_t     Rx_Notification_Status;
  BLUZ_APP_SendInformation_t     Tx_Notification_Status;
  /* USER CODE BEGIN Service1_APP_Context_t */

  /* USER CODE END Service1_APP_Context_t */
  uint16_t              ConnectionHandle;
} BLUZ_APP_Context_t;

/* Private defines -----------------------------------------------------------*/
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* External variables --------------------------------------------------------*/
/* USER CODE BEGIN EV */
extern uint16_t MTUSizeValue;
/* USER CODE END EV */

/* Private macros ------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
static BLUZ_APP_Context_t BLUZ_APP_Context;

uint8_t a_BLUZ_UpdateCharData[247];

/* USER CODE BEGIN PV */
typedef struct
{
  BLUZ_Data_t TxData;
  uint8_t connectionstatus;
} BLUZ_App_Context_t;

BLUZ_App_Context_t BZ_Context;
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
static void BLUZ_Rx_SendNotification(void);
static void BLUZ_Tx_SendNotification(void);

/* USER CODE BEGIN PFP */
//static uint8_t Notification_Data_Buffer[DATA_NOTIFICATION_MAX_PACKET_SIZE]; /* DATA_NOTIFICATION_MAX_PACKET_SIZE data + CRC */
/* USER CODE END PFP */

/* Functions Definition ------------------------------------------------------*/
void BLUZ_Notification(BLUZ_NotificationEvt_t *p_Notification)
{
  /* USER CODE BEGIN Service1_Notification_1 */

  /* USER CODE END Service1_Notification_1 */
  switch(p_Notification->EvtOpcode)
  {
    /* USER CODE BEGIN Service1_Notification_Service1_EvtOpcode */

    /* USER CODE END Service1_Notification_Service1_EvtOpcode */

    case BLUZ_RX_NOTIFY_ENABLED_EVT:
      /* USER CODE BEGIN Service1Char1_NOTIFY_ENABLED_EVT */
    	connectFlag = true;
      	//UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_DISABLE);
    	//connectFlag = true;
    	//MX_GPIO_Init();
    	//HAL_GPIO_WritePin(LED_GPIO_Port, LED_Pin, GPIO_PIN_SET);
      /* USER CODE END Service1Char1_NOTIFY_ENABLED_EVT */
      break;

    case BLUZ_RX_NOTIFY_DISABLED_EVT:
      /* USER CODE BEGIN Service1Char1_NOTIFY_DISABLED_EVT */
    	connectFlag = false;
      /* USER CODE END Service1Char1_NOTIFY_DISABLED_EVT */
      break;

    case BLUZ_TX_WRITE_NO_RESP_EVT:
      /* USER CODE BEGIN Service1Char2_WRITE_NO_RESP_EVT */
    	void calcPulseLevel(void);
    	/* Прием данных со смартфона */
		if (p_Notification->DataTransfered.Length > 10) {
			/* Прием данных */
			if (p_Notification->DataTransfered.p_Payload[0] == (uint8_t) '<'
			  && p_Notification->DataTransfered.p_Payload[1] == (uint8_t) 'S'
			  && p_Notification->DataTransfered.p_Payload[2] == (uint8_t) '>') {
				/* Проверка контрольной суммы */
				uint16_t checkSumm = 0;
				for (int iii = 0; iii < p_Notification->DataTransfered.Length - 3; iii++) {
					checkSumm = checkSumm + p_Notification->DataTransfered.p_Payload[iii];
				}
				/* Контрольная сумма в передаче */
				uint16_t checkSummTest = p_Notification->DataTransfered.p_Payload[242] | (uint16_t) p_Notification->DataTransfered.p_Payload[243] << 8;

				/* Сравниваем контрольные суммы */
				if (checkSummTest == checkSumm) {
					#ifdef DEBUG_USER
					bzero((char *) uartBuffer, sizeof(uartBuffer));
					sprintf(uartBuffer, "CS correct: %u, Ln: %u\n\r", checkSumm, p_Notification->DataTransfered.Length);
					HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
					#endif
                    /*
                    * Формат буфера настроек
                    *
                    * 0,1,2         - Маркер <S>
                    * 3             - Режим
                    *                   0 - Настройки
                    *                   1 - Команда очистки буфера спектра
                    *                   2 - Запуск/Останов спектрометра
                    *                   3 - Очистка буфера дозиметра
                    *                   4 - Очистка лога
                    *                   5 - Запрос на передачу спектра истории превышений
                    *
                    * 4,5,6,7       - Первый порог в uR
                    * 8,9,10,11     - Второй порог в uR
                    * 12,13,14,15   - Третий порог в uR
                    * 16,17,18,19   - Коэффициент пересчета CPS в uR
                    * 20            - Битовые флаги управления светодиодом, звуком и вибро
                    *                   0 - Светодиодная индикация прихода частицы (1 - включена, 0 - выключена)
                    *                   1 - Звуковое сопровождение прихода частицы  (1 - включено, 0 - выключено)
                    *                   2 - Звуковая сигнализация 1 порог   (1 - включено, 0 - выключено)
                    *                   3 - Звуковая сигнализация 2 порог   (1 - включено, 0 - выключено)
                    *                   4 - Звуковая сигнализация 3 порог   (1 - включено, 0 - выключено)
                    *                   5 - Вибро сигнализация 1 порог   (1 - включено, 0 - выключено)
                    *                   6 - Вибро сигнализация 2 порог   (1 - включено, 0 - выключено)
                    *                   7 - Вибро сигнализация 3 порог   (1 - включено, 0 - выключено)
                    * 21,22,23,24   - Коэффициент A полинома преобразования канала в энергию.
                    * 25,26,27,28   - Коэффициент B полинома преобразования канала в энергию.
                    * 29,30,31,32   - Коэффициент C полинома преобразования канала в энергию.
                    * 33,34         - Уровень высокого напряжения
                    * 35,36         - Уровень компаратора
                    * 37			- Разрешение. 0 - 1024, 1 - 2048, 2 - 4096
                    * 38			- Битовые флаги управления прибором
                    * 					0 - Запуск набора спектра при включении.
                    * 					1 -|
                    * 					2 -| Время выборки АЦП - 3 бита от 0 до 7
                    * 					3 -|
                    * 					4 -
                    * 					5 -
                    * 					6 -
                    * 					7 -
                    * 63, 64		- Точность расчета для дозиметра. Количество импульсов в усреднении.
                    *
                    * 242, 243      - Контрольная сумма
                    */
					if (p_Notification->DataTransfered.p_Payload[3] == cmd_setup) {							// 0 - Настройки
						LEDEnable = p_Notification->DataTransfered.p_Payload[20] & 0b00000001;				// LED
						SoundEnable = p_Notification->DataTransfered.p_Payload[20] & 0b00000010;			// Sound
						levelSound1 = p_Notification->DataTransfered.p_Payload[20] & 0b00000100;			// Звук для первого порога
						levelSound2 = p_Notification->DataTransfered.p_Payload[20] & 0b00001000;			// Звук для второго порога
						levelSound3 = p_Notification->DataTransfered.p_Payload[20] & 0b00010000;			// Звук для третьего порога
						levelVibro1 = p_Notification->DataTransfered.p_Payload[20] & 0b00100000;			// Вибро для первого порога
						levelVibro2 = p_Notification->DataTransfered.p_Payload[20] & 0b01000000;			// Вибро для второго порога
						levelVibro3 = p_Notification->DataTransfered.p_Payload[20] & 0b10000000;			// Вибро для третьего порога
						autoStartSpecrometr = p_Notification->DataTransfered.p_Payload[38] & 0b00000001;	// Запуск набора спектра при включении
						uint8_t tmpSample = (p_Notification->DataTransfered.p_Payload[38] >> 1) & 0x7;		// Время выборки АЦП (0..7)

						/* Уровень порога 1 в uR/h */
						level1 = p_Notification->DataTransfered.p_Payload[7]
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[6] << 8) & 0x0000FF00)
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[5] << 16) & 0x00FF0000)
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[4] << 24)  & 0xFF000000);

						/* Уровень порога 2 в uR/h */
						level2 = p_Notification->DataTransfered.p_Payload[11]
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[10] << 8) & 0x0000FF00)
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[9] << 16) & 0x00FF0000)
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[8] << 24)  & 0xFF000000);

						/* Уровень порога 3 в uR/h */
						level3 = p_Notification->DataTransfered.p_Payload[15]
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[14] << 8) & 0x0000FF00)
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[13] << 16) & 0x00FF0000)
							| (((uint32_t) p_Notification->DataTransfered.p_Payload[12] << 24)  & 0xFF000000);

						/* Коэффициент пересчета CPS в uR/h */
						calcCoeff.Uint[3] = p_Notification->DataTransfered.p_Payload[16];
						calcCoeff.Uint[2] = p_Notification->DataTransfered.p_Payload[17];
						calcCoeff.Uint[1] = p_Notification->DataTransfered.p_Payload[18];
						calcCoeff.Uint[0] = p_Notification->DataTransfered.p_Payload[19];

						/* Коэффициент A полинома преобразования канала в энергию для 1024 */
						enCoefA1024.Uint[3] = p_Notification->DataTransfered.p_Payload[21];
						enCoefA1024.Uint[2] = p_Notification->DataTransfered.p_Payload[22];
						enCoefA1024.Uint[1] = p_Notification->DataTransfered.p_Payload[23];
						enCoefA1024.Uint[0] = p_Notification->DataTransfered.p_Payload[24];

						/* Коэффициент B полинома преобразования канала в энергию для 1024 */
						enCoefB1024.Uint[3] = p_Notification->DataTransfered.p_Payload[25];
						enCoefB1024.Uint[2] = p_Notification->DataTransfered.p_Payload[26];
						enCoefB1024.Uint[1] = p_Notification->DataTransfered.p_Payload[27];
						enCoefB1024.Uint[0] = p_Notification->DataTransfered.p_Payload[28];

						/* Коэффициент C полинома преобразования канала в энергию для 1024 */
						enCoefC1024.Uint[3] = p_Notification->DataTransfered.p_Payload[29];
						enCoefC1024.Uint[2] = p_Notification->DataTransfered.p_Payload[30];
						enCoefC1024.Uint[1] = p_Notification->DataTransfered.p_Payload[31];
						enCoefC1024.Uint[0] = p_Notification->DataTransfered.p_Payload[32];

						/* Коэффициент A полинома преобразования канала в энергию для 2048 */
						enCoefA2048.Uint[3] = p_Notification->DataTransfered.p_Payload[39];
						enCoefA2048.Uint[2] = p_Notification->DataTransfered.p_Payload[40];
						enCoefA2048.Uint[1] = p_Notification->DataTransfered.p_Payload[41];
						enCoefA2048.Uint[0] = p_Notification->DataTransfered.p_Payload[42];

						/* Коэффициент B полинома преобразования канала в энергию для 2048 */
						enCoefB2048.Uint[3] = p_Notification->DataTransfered.p_Payload[43];
						enCoefB2048.Uint[2] = p_Notification->DataTransfered.p_Payload[44];
						enCoefB2048.Uint[1] = p_Notification->DataTransfered.p_Payload[45];
						enCoefB2048.Uint[0] = p_Notification->DataTransfered.p_Payload[46];

						/* Коэффициент C полинома преобразования канала в энергию для 2048 */
						enCoefC2048.Uint[3] = p_Notification->DataTransfered.p_Payload[47];
						enCoefC2048.Uint[2] = p_Notification->DataTransfered.p_Payload[48];
						enCoefC2048.Uint[1] = p_Notification->DataTransfered.p_Payload[49];
						enCoefC2048.Uint[0] = p_Notification->DataTransfered.p_Payload[50];

						/* Коэффициент A полинома преобразования канала в энергию для 4096 */
						enCoefA4096.Uint[3] = p_Notification->DataTransfered.p_Payload[51];
						enCoefA4096.Uint[2] = p_Notification->DataTransfered.p_Payload[52];
						enCoefA4096.Uint[1] = p_Notification->DataTransfered.p_Payload[53];
						enCoefA4096.Uint[0] = p_Notification->DataTransfered.p_Payload[54];

						/* Коэффициент B полинома преобразования канала в энергию для 4096 */
						enCoefB4096.Uint[3] = p_Notification->DataTransfered.p_Payload[55];
						enCoefB4096.Uint[2] = p_Notification->DataTransfered.p_Payload[56];
						enCoefB4096.Uint[1] = p_Notification->DataTransfered.p_Payload[57];
						enCoefB4096.Uint[0] = p_Notification->DataTransfered.p_Payload[58];

						/* Коэффициент C полинома преобразования канала в энергию для 4096 */
						enCoefC4096.Uint[3] = p_Notification->DataTransfered.p_Payload[59];
						enCoefC4096.Uint[2] = p_Notification->DataTransfered.p_Payload[60];
						enCoefC4096.Uint[1] = p_Notification->DataTransfered.p_Payload[61];
						enCoefC4096.Uint[0] = p_Notification->DataTransfered.p_Payload[62];

						/* Точность усреднения для дозиметра, количество импульсов */
						dozimetrAquracy = p_Notification->DataTransfered.p_Payload[63] | ((uint16_t) p_Notification->DataTransfered.p_Payload[64] << 8);

						/* Разрядность канала */
						if (p_Notification->DataTransfered.p_Payload[65] < 16 || p_Notification->DataTransfered.p_Payload[65] > 32) {
							bitsOfChannal = CAPCHAN;
						} else if (bitsOfChannal != (uint32_t) p_Notification->DataTransfered.p_Payload[65]) {
							bitsOfChannal = (uint32_t) p_Notification->DataTransfered.p_Payload[65];
							CoefChan = 65535.0 / (double) bitsOfChannal;
						}

						/* Уровни компаратора и высокого напряжения */
						HVoltage = p_Notification->DataTransfered.p_Payload[33] | (p_Notification->DataTransfered.p_Payload[34] << 8);
						comparatorLevel = p_Notification->DataTransfered.p_Payload[35] | (p_Notification->DataTransfered.p_Payload[36] << 8);

						/* Разрешение спектра. 0 - 1024, 1 - 2048, 2 - 4096 */
						if ((resolutionSpecter != (spectrResolution_t) p_Notification->DataTransfered.p_Payload[37]) || currentSamplingTime != tmpSample) {
							resolutionSpecter = (spectrResolution_t) p_Notification->DataTransfered.p_Payload[37];
							/* Время выборки изменяется - нужно очистить спектр */
							if (currentSamplingTime != tmpSample) {
								for (int iii = 0; iii < MAX_RESOLUTION; iii++) {
									tmpSpecterBuffer[iii] = 0;
								}
								logUpdate(resSpectrometerLog);
							}
							currentSamplingTime = tmpSample;
							spectrometerPulse = 0;
							spectrometerTime = 0;
							/* Если включен спектрометр, нужно установить тип передачи */
							if (dataType > onlyDozimeter) {
								switch (resolutionSpecter) {
								case resolution1024:
									dataType = dozimeterSpecter1024;
									break;
								case resolution2048:
									dataType = dozimeterSpecter2048;
									break;
								case resolution4096:
									dataType = dozimeterSpecter4096;
									break;
								default:
									dataType = dozimeterSpecter1024;
									resolutionSpecter = resolution1024;
									break;
								}
							}
						}

						/* Вывод конфига для отладки */
						#ifdef DEBUG_USER
						bzero((char *) uartBuffer, sizeof(uartBuffer));
						sprintf(uartBuffer, "CompLev: %u, HVolt: %u, Koef: %f, Res: %u\n\r", comparatorLevel, HVoltage, calcCoeff.Float, resolution);
						HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
						bzero((char *) uartBuffer, sizeof(uartBuffer));
						sprintf(uartBuffer, "CoefA: %f, CoefB: %f, CoefC: %f\n\r", enCoefA.Float, enCoefB.Float, enCoefC.Float);
						HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
						bzero((char *) uartBuffer, sizeof(uartBuffer));
						sprintf(uartBuffer, "Lev1: %d, Lev2: %d, Lev3: %d\n\r", level1, level2, level3);
						HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
						bzero((char *) uartBuffer, sizeof(uartBuffer));
						sprintf(uartBuffer, "calcCoeff: %f\n\r", calcCoeff.Float);
						HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
						#endif

						/* Установка уровней для компаратора и высокого напряжения */
						setLevelOnPort(CHANNEL_A, comparatorLevel);
						setLevelOnPort(CHANNEL_B, HVoltage);
						/*
						 * TODO -- не получается разобраться, почему запись выполняется со второго раза.
						 * Первый вызов стирает Flash и только на втором происходит запись.
						 *
						 */
						writeFlash();
						calcPulseLevel();	 // Обновление порогов
						logUpdate(writeFlashLog);

					/* Очистка буфера спектра */
					} else if (p_Notification->DataTransfered.p_Payload[3] == cmd_clear_specter) {			// Очистка буфера спектрометра
						for (int iii = 0; iii < MAX_RESOLUTION; iii++) {
							tmpSpecterBuffer[iii] = 0;
						}
						spectrometerPulse = 0;
						spectrometerTime = 0;
						logUpdate(writeFlashLog);
					/* Включение/выключение спектрометра */
					} else if (p_Notification->DataTransfered.p_Payload[3] == cmd_startup_spectrometer) {	// Запуск/останов спектрометра
						HAL_ADC_Stop_DMA(&hadc4);
						HAL_ADC_DeInit(&hadc4);
						//HAL_DMA_DeInit(&handle_GPDMA1_Channel0);
						if (dataType == onlyDozimeter) {													// Переключение в режим спектрометра
							switch (resolutionSpecter) {
							case resolution1024:
								dataType = dozimeterSpecter1024;
								break;
							case resolution2048:
								dataType = dozimeterSpecter2048;
								break;
							case resolution4096:
								dataType = dozimeterSpecter4096;
								break;
							default:
								dataType = dozimeterSpecter1024;
								resolutionSpecter = resolution1024;
								break;
							}
							MX_ADC4_Init();
							//MX_GPDMA1_Init();
							HAL_ADC_Start_DMA(&hadc4, TVLevel, 1);
							//hadc4.DMA_Handle->Instance->CCR &= ~DMA_IT_HT;
							logUpdate(startSpectrometerLog);
							/* Включим ADC для одного канала */
							//MODIFY_REG(hadc4.Instance->CHSELR, ADC_CHSELR_SQ_ALL, ((ADC_CHSELR_SQ2 | ADC_CHSELR_SQ3 | ADC_CHSELR_SQ4 | ADC_CHSELR_SQ5 | ADC_CHSELR_SQ6 | ADC_CHSELR_SQ7 | ADC_CHSELR_SQ8) << (((1UL - 1UL) * ADC_REGULAR_RANK_2) & 0x1FUL)) | (hadc4.ADCGroupRegularSequencerRanks));
						} else {
							dataType = onlyDozimeter;												// Переключение в режим дозиметра
							MX_ADC4_Init();
							logUpdate(stopSpectrometerLog);
						}
					/* Сброс дозиметра */
					} else if (p_Notification->DataTransfered.p_Payload[3] == cmd_clear_dosimeter) {
						pulseCounter = 0;
						currentTime = 0;
						//memset(dozimetrBuffer, 0, SIZE_DOZIMETR_BUFER * 2);
						for (int ii = 0; ii < SIZE_DOZIMETR_BUFER; ii++) {
							dozimetrBuffer[ii] = 0;
						}
						logUpdate(resDozimeterLog);
					/* Очистка лога */
					} else if (p_Notification->DataTransfered.p_Payload[3] == cmd_clear_logs) {
						for (int ii = 0; ii < LOG_BUFER_SIZE; ii++) {
							logBuffer[ii].time = 0;
							logBuffer[ii].type = 0;
						}
						logUpdate(clearLog);
					/* Запрос на передачу спектра истории */
					} else if (p_Notification->DataTransfered.p_Payload[3] == cmd_history_request) {
						historyRequest = true;
						interval2 = 0;
					/* Включение звука и вибро, для поиска прибора */
					} else if (p_Notification->DataTransfered.p_Payload[3] == cmd_find_device) {
						NotifyAct(SOUND_NOTIFY | VIBRO_NOTIFY, 5);
					}
				} else {
				}
			}

			/* Вывод приемного буфера */
			/*
			bzero((char *) uartBuffer, sizeof(uartBuffer));
			sprintf(uartBuffer, "bz_rx_app: DataTransfered\n\r");
			HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
			for (int iii = 0; iii < p_Notification->DataTransfered.Length; iii++) {
				sprintf(uartBuffer, "%02X ", p_Notification->DataTransfered.p_Payload[iii]);
				HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, 3, 100);
			}
			HAL_UART_Transmit(&huart2, (uint8_t *) "\n\r", 2, 100);
			*/
		}




      /* USER CODE END Service1Char2_WRITE_NO_RESP_EVT */
      break;

    case BLUZ_TX_NOTIFY_ENABLED_EVT:
      /* USER CODE BEGIN Service1Char2_NOTIFY_ENABLED_EVT */

      /* USER CODE END Service1Char2_NOTIFY_ENABLED_EVT */
      break;

    case BLUZ_TX_NOTIFY_DISABLED_EVT:
      /* USER CODE BEGIN Service1Char2_NOTIFY_DISABLED_EVT */

      /* USER CODE END Service1Char2_NOTIFY_DISABLED_EVT */
      break;

    default:
      /* USER CODE BEGIN Service1_Notification_default */

      /* USER CODE END Service1_Notification_default */
      break;
  }
  /* USER CODE BEGIN Service1_Notification_2 */

  /* USER CODE END Service1_Notification_2 */
  return;
}

void BLUZ_APP_EvtRx(BLUZ_APP_ConnHandleNotEvt_t *p_Notification)
{
  /* USER CODE BEGIN Service1_APP_EvtRx_1 */

  /* USER CODE END Service1_APP_EvtRx_1 */

  switch(p_Notification->EvtOpcode)
  {
    /* USER CODE BEGIN Service1_APP_EvtRx_Service1_EvtOpcode */

    /* USER CODE END Service1_APP_EvtRx_Service1_EvtOpcode */
    case BLUZ_CONN_HANDLE_EVT :
      /* USER CODE BEGIN Service1_APP_CONN_HANDLE_EVT */
      	UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_DISABLE);
      	/*
        * Установка параметров интерфейса
        *  2 M PHY
        */

       /* 50 ms, 0 latency, 500 ms timeout */
       //aci_l2cap_connection_parameter_update_req(p_Notification->ConnectionHandle,
       //    40, 40,   // 50 ms
       //    0,        // latency
       //    500);     // timeout

      	hci_le_set_phy(p_Notification->ConnectionHandle, 0, HCI_TX_PHYS_LE_1M_PREF, HCI_RX_PHYS_LE_1M_PREF, 0);
      	//hci_le_set_phy(p_Notification->ConnectionHandle, 0, HCI_TX_PHYS_LE_2M_PREF, HCI_RX_PHYS_LE_2M_PREF, 0);

      /* USER CODE END Service1_APP_CONN_HANDLE_EVT */
      break;

    case BLUZ_DISCON_HANDLE_EVT :
      /* USER CODE BEGIN Service1_APP_DISCON_HANDLE_EVT */

      /* USER CODE END Service1_APP_DISCON_HANDLE_EVT */
      break;

    default:
      /* USER CODE BEGIN Service1_APP_EvtRx_default */

      /* USER CODE END Service1_APP_EvtRx_default */
      break;
  }

  /* USER CODE BEGIN Service1_APP_EvtRx_2 */

  /* USER CODE END Service1_APP_EvtRx_2 */

  return;
}

void BLUZ_APP_Init(void)
{
  UNUSED(BLUZ_APP_Context);
  BLUZ_Init();

  /* USER CODE BEGIN Service1_APP_Init */

  /* USER CODE END Service1_APP_Init */
  return;
}

/* USER CODE BEGIN FD */

/* USER CODE END FD */

/*************************************************************
 *
 * LOCAL FUNCTIONS
 *
 *************************************************************/
__USED void BLUZ_Rx_SendNotification(void) /* Property Notification */
{
  BLUZ_APP_SendInformation_t notification_on_off = Rx_NOTIFICATION_OFF;
  BLUZ_Data_t bluz_notification_data;

  bluz_notification_data.p_Payload = (uint8_t*)a_BLUZ_UpdateCharData;
  bluz_notification_data.Length = 0;

  /* USER CODE BEGIN Service1Char1_NS_1 */

  /* USER CODE END Service1Char1_NS_1 */

  if (notification_on_off != Rx_NOTIFICATION_OFF)
  {
    BLUZ_UpdateValue(BLUZ_RX, &bluz_notification_data);
  }

  /* USER CODE BEGIN Service1Char1_NS_Last */

  /* USER CODE END Service1Char1_NS_Last */

  return;
}

__USED void BLUZ_Tx_SendNotification(void) /* Property Notification */
{
  BLUZ_APP_SendInformation_t notification_on_off = Tx_NOTIFICATION_OFF;
  BLUZ_Data_t bluz_notification_data;

  bluz_notification_data.p_Payload = (uint8_t*)a_BLUZ_UpdateCharData;
  bluz_notification_data.Length = 0;

  /* USER CODE BEGIN Service1Char2_NS_1 */

  /* USER CODE END Service1Char2_NS_1 */

  if (notification_on_off != Tx_NOTIFICATION_OFF)
  {
    BLUZ_UpdateValue(BLUZ_TX, &bluz_notification_data);
  }

  /* USER CODE BEGIN Service1Char2_NS_Last */

  /* USER CODE END Service1Char2_NS_Last */

  return;
}

/* USER CODE BEGIN FD_LOCAL_FUNCTIONS */
void BleStackCB_Process(void);

uint8_t sendData(uint8_t *data)
{
    if (!connectFlag) return 0;

    BZ_Context.TxData.p_Payload = data;
    BZ_Context.TxData.Length    = MTUSizeValue;

    tBleStatus ret;
    do {
        ret = BLUZ_UpdateValue(BLUZ_RX, &BZ_Context.TxData);
        if (ret == BLE_STATUS_BUSY) {          // HCI-команда не ушла
            MX_APPE_Process();                 // прокручиваем стек
        }
    } while (ret == BLE_STATUS_BUSY);

    return (ret == BLE_STATUS_SUCCESS) ? 1 : 0;
}
/*
void sendData( uint8_t *dataSpectrBufer ) {
	if (connectFlag && tx_pool_ready) {
	  tBleStatus status = BLE_STATUS_INVALID_PARAMS;
		//for (int iii = 0; iii < MTUSizeValue; iii++) {
		//	Notification_Data_Buffer[iii] = dataSpectrBufer[iii];
		//}
		//BZ_Context.TxData.p_Payload = Notification_Data_Buffer;
		BZ_Context.TxData.p_Payload = dataSpectrBufer;
		BZ_Context.TxData.Length =  MTUSizeValue;

		status = BLUZ_UpdateValue(BLUZ_RX, (BLUZ_Data_t *) &BZ_Context.TxData);

		if (status == BLE_STATUS_INSUFFICIENT_RESOURCES) {
			#ifdef DEBUG_USER
			bzero((char *) uartBuffer, sizeof(uartBuffer));
			sprintf(uartBuffer, "bz_rx_app: Error transfer with status: %d\n\r", status);
			HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
			#endif
		} else {
			//bzero((char *) uartBuffer, sizeof(uartBuffer));
			//sprintf(uartBuffer, "bz_rx_app: Send complete\n\r");
			//HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
		}

	  BleStackCB_Process();
	}
  return;
}

*/
/* USER CODE END FD_LOCAL_FUNCTIONS */
