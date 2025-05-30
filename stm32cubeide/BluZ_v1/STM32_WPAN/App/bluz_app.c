/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    service1_app.c
  * @author  MCD Application Team
  * @brief   service1_app application definition.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2024 STMicroelectronics.
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
#include "app_ble.h"
#include "ll_sys_if.h"
#include "dbg_trace.h"
#include "ble.h"
#include "bluz_app.h"
#include "bluz.h"
#include "stm32_seq.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "LTC1662.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */
extern char uartBuffer[400];
typedef struct
{
  BLUZ_Data_t TxData;
  uint8_t connectionstatus;
} BLUZ_App_Context_t;

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
BLUZ_App_Context_t BZ_Context;
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
static void BLUZ_Rx_SendNotification(void);
static void BLUZ_Tx_SendNotification(void);

/* USER CODE BEGIN PFP */
static uint8_t Notification_Data_Buffer[DATA_NOTIFICATION_MAX_PACKET_SIZE]; /* DATA_NOTIFICATION_MAX_PACKET_SIZE data + CRC */
/* USER CODE END PFP */

/* Functions Definition ------------------------------------------------------*/
void BLUZ_Notification(BLUZ_NotificationEvt_t *p_Notification)
{
  /* USER CODE BEGIN Service1_Notification_1 */
	#ifdef DEBUG_USER
	bzero((char *) uartBuffer, sizeof(uartBuffer));
	sprintf(uartBuffer, "Service1_Notification_1\n\r");
	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	#endif
  /* USER CODE END Service1_Notification_1 */
  switch(p_Notification->EvtOpcode)
  {
    /* USER CODE BEGIN Service1_Notification_Service1_EvtOpcode */
  /*
	bzero((char *) uartBuffer, sizeof(uartBuffer));
	sprintf(uartBuffer, "Service1_Notification_Service1_EvtOpcode\n\r");
	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
*/
    /* USER CODE END Service1_Notification_Service1_EvtOpcode */

    case BLUZ_RX_NOTIFY_ENABLED_EVT:
      /* USER CODE BEGIN Service1Char1_NOTIFY_ENABLED_EVT */
    	connectFlag = true;
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1Char1_NOTIFY_ENABLED_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif
      /* USER CODE END Service1Char1_NOTIFY_ENABLED_EVT */
      break;

    case BLUZ_RX_NOTIFY_DISABLED_EVT:
      /* USER CODE BEGIN Service1Char1_NOTIFY_DISABLED_EVT */
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1Char1_NOTIFY_DISABLED_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif
      /* USER CODE END Service1Char1_NOTIFY_DISABLED_EVT */
      break;

    case BLUZ_TX_WRITE_NO_RESP_EVT:
      /* USER CODE BEGIN Service1Char2_WRITE_NO_RESP_EVT */
    	/* Прием данных со смартфона */
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1Char2_WRITE_NO_RESP_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif
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
                    * 					1 -
                    * 					2 -
                    * 					3 -
                    * 					4 -
                    * 					5 -
                    * 					6 -
                    * 					7 -
                    *
                    * 242, 243      - Контрольная сумма
                    */
					if (p_Notification->DataTransfered.p_Payload[3] == 0) {									// 0 - Настройки
						LEDEnable = p_Notification->DataTransfered.p_Payload[20] & 0b00000001;				// LED
						SoundEnable = p_Notification->DataTransfered.p_Payload[20] & 0b00000010;			// Sound
						levelSound1 = p_Notification->DataTransfered.p_Payload[20] & 0b00000100;			// Звук для первого порога
						levelSound2 = p_Notification->DataTransfered.p_Payload[20] & 0b00001000;			// Звук для второго порога
						levelSound3 = p_Notification->DataTransfered.p_Payload[20] & 0b00010000;			// Звук для третьего порога
						levelVibro1 = p_Notification->DataTransfered.p_Payload[20] & 0b00100000;			// Вибро для первого порога
						levelVibro2 = p_Notification->DataTransfered.p_Payload[20] & 0b01000000;			// Вибро для второго порога
						levelVibro3 = p_Notification->DataTransfered.p_Payload[20] & 0b10000000;			// Вибро для третьего порога
						autoStartSpecrometr = p_Notification->DataTransfered.p_Payload[38] & 0b00000001;	// Запуск набора спектра при включении

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

						/* Коэффициент A полинома преобразования канала в энергию */
						enCoefA.Uint[3] = p_Notification->DataTransfered.p_Payload[21];
						enCoefA.Uint[2] = p_Notification->DataTransfered.p_Payload[22];
						enCoefA.Uint[1] = p_Notification->DataTransfered.p_Payload[23];
						enCoefA.Uint[0] = p_Notification->DataTransfered.p_Payload[24];

						/* Коэффициент B полинома преобразования канала в энергию */
						enCoefB.Uint[3] = p_Notification->DataTransfered.p_Payload[25];
						enCoefB.Uint[2] = p_Notification->DataTransfered.p_Payload[26];
						enCoefB.Uint[1] = p_Notification->DataTransfered.p_Payload[27];
						enCoefB.Uint[0] = p_Notification->DataTransfered.p_Payload[28];

						/* Коэффициент C полинома преобразования канала в энергию */
						enCoefC.Uint[3] = p_Notification->DataTransfered.p_Payload[29];
						enCoefC.Uint[2] = p_Notification->DataTransfered.p_Payload[30];
						enCoefC.Uint[1] = p_Notification->DataTransfered.p_Payload[31];
						enCoefC.Uint[0] = p_Notification->DataTransfered.p_Payload[32];


						/* Уровни компаратора и высокого напряжения */
						HVoltage = p_Notification->DataTransfered.p_Payload[33] | p_Notification->DataTransfered.p_Payload[34] << 8;
						comparatorLevel = p_Notification->DataTransfered.p_Payload[35] | p_Notification->DataTransfered.p_Payload[36] << 8;

						/* Разрешение спектра. 0 - 1024, 1 - 2048, 2 - 4096 */
						resolution = p_Notification->DataTransfered.p_Payload[37];

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
						//writeFlash();

					/* Очистка буфера спектра */
					} else if (p_Notification->DataTransfered.p_Payload[3] == 1) {
						for (int iii = 0; iii < MAX_RESOLUTION; iii++) {
							tmpSpecterBuffer[iii] = 0;
						}
						pulseCounter = 0;
						currentTime = 0;
					}
				} else {
					#ifdef DEBUG_USER
					bzero((char *) uartBuffer, sizeof(uartBuffer));
					sprintf(uartBuffer, "CS incorrect, calcCS: %u, CS: %u Ln: %u\n\r", checkSumm, checkSummTest, p_Notification->DataTransfered.Length);
					HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
					#endif
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
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1Char2_NOTIFY_ENABLED_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif
      /* USER CODE END Service1Char2_NOTIFY_ENABLED_EVT */
      break;

    case BLUZ_TX_NOTIFY_DISABLED_EVT:
      /* USER CODE BEGIN Service1Char2_NOTIFY_DISABLED_EVT */
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1Char2_NOTIFY_DISABLED_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif
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
	#ifdef DEBUG_USER
	bzero((char *) uartBuffer, sizeof(uartBuffer));
	sprintf(uartBuffer, "Service1_APP_EvtRx_1\n\r");
	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	#endif

  /* USER CODE END Service1_APP_EvtRx_1 */

  switch(p_Notification->EvtOpcode)
  {
    /* USER CODE BEGIN Service1_APP_EvtRx_Service1_EvtOpcode */

    /* USER CODE END Service1_APP_EvtRx_Service1_EvtOpcode */
    case BLUZ_CONN_HANDLE_EVT :
      /* USER CODE BEGIN Service1_APP_CONN_HANDLE_EVT */
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1_APP_CONN_HANDLE_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif

      /* USER CODE END Service1_APP_CONN_HANDLE_EVT */
      break;

    case BLUZ_DISCON_HANDLE_EVT :
      /* USER CODE BEGIN Service1_APP_DISCON_HANDLE_EVT */
		#ifdef DEBUG_USER
    	bzero((char *) uartBuffer, sizeof(uartBuffer));
    	sprintf(uartBuffer, "Service1_APP_DISCON_HANDLE_EVT\n\r");
    	HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif

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

  /* USER CODE BEGIN Service1Char1_NS_1*/

  /* USER CODE END Service1Char1_NS_1*/

  if (notification_on_off != Rx_NOTIFICATION_OFF)
  {
    BLUZ_UpdateValue(BLUZ_RX, &bluz_notification_data);
  }

  /* USER CODE BEGIN Service1Char1_NS_Last*/

  /* USER CODE END Service1Char1_NS_Last*/

  return;
}

__USED void BLUZ_Tx_SendNotification(void) /* Property Notification */
{
  BLUZ_APP_SendInformation_t notification_on_off = Tx_NOTIFICATION_OFF;
  BLUZ_Data_t bluz_notification_data;

  bluz_notification_data.p_Payload = (uint8_t*)a_BLUZ_UpdateCharData;
  bluz_notification_data.Length = 0;

  /* USER CODE BEGIN Service1Char2_NS_1*/

  /* USER CODE END Service1Char2_NS_1*/

  if (notification_on_off != Tx_NOTIFICATION_OFF)
  {
    BLUZ_UpdateValue(BLUZ_TX, &bluz_notification_data);
  }

  /* USER CODE BEGIN Service1Char2_NS_Last*/

  /* USER CODE END Service1Char2_NS_Last*/

  return;
}

/* USER CODE BEGIN FD_LOCAL_FUNCTIONS*/
void sendData( uint8_t *dataSpectrBufer )
{
	if (connectFlag) {
	  tBleStatus status = BLE_STATUS_INVALID_PARAMS;
	  //uint8_t crc_result;

		/*Data Packet to send to remote*/
		//Notification_Data_Buffer[0] += 1;
		/* compute CRC */
		//crc_result = APP_BLE_ComputeCRC8((uint8_t*) Notification_Data_Buffer, (MTUSizeValue - 1));
		//Notification_Data_Buffer[MTUSizeValue - 1] = crc_result;
		for (int iii = 0; iii < MTUSizeValue; iii++) {
			Notification_Data_Buffer[iii] = dataSpectrBufer[iii];
		}
		BZ_Context.TxData.p_Payload = Notification_Data_Buffer;
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

/* USER CODE END FD_LOCAL_FUNCTIONS*/
