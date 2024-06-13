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

/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */
extern char uartBuffer[100];
typedef struct
{
  BLUZ_Data_t TxData;
  uint8_t connectionstatus;
} BLUZ_App_Context_t;

/* USER CODE END PTD */

typedef enum
{
  Tx_NOTIFICATION_OFF,
  Tx_NOTIFICATION_ON,
  /* USER CODE BEGIN Service1_APP_SendInformation_t */

  /* USER CODE END Service1_APP_SendInformation_t */
  BLUZ_APP_SENDINFORMATION_LAST
} BLUZ_APP_SendInformation_t;

typedef struct
{
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
static void BLUZ_Tx_SendNotification(void);

/* USER CODE BEGIN PFP */
static uint8_t Notification_Data_Buffer[DATA_NOTIFICATION_MAX_PACKET_SIZE]; /* DATA_NOTIFICATION_MAX_PACKET_SIZE data + CRC */
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

    case BLUZ_RX_READ_EVT:
      /* USER CODE BEGIN Service1Char1_READ_EVT */

      /* USER CODE END Service1Char1_READ_EVT */
      break;

    case BLUZ_RX_WRITE_NO_RESP_EVT:
      /* USER CODE BEGIN Service1Char1_WRITE_NO_RESP_EVT */
    	/* Прием данных со смартфона */
		#pragma message ("Warning receive data from smartPhone")
    	  if (p_Notification->DataTransfered.Length > 10) {
    		bzero((char *) uartBuffer, sizeof(uartBuffer));
    		sprintf(uartBuffer, "bz_rx_app: DataTransfered\n\r");
    		//HAL_UART_Transmit(&huart1, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
    		for (int iii = 0; iii < p_Notification->DataTransfered.Length; iii++) {
    			sprintf(uartBuffer, "%02X ", p_Notification->DataTransfered.p_Payload[iii]);
    			//HAL_UART_Transmit(&huart1, (uint8_t *) uartBuffer, 3, 100);
    		}
    		//HAL_UART_Transmit(&huart1, (uint8_t *) "\n\r", 2, 100);
  		/* Нужно очистить приемный буфер */
  		//bzero(p_Notification->DataTransfered.p_Payload, p_Notification->DataTransfered.Length);
    	  }
      /* USER CODE END Service1Char1_WRITE_NO_RESP_EVT */
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
void SendData( void )
{
	if (connectFlag) {
	  tBleStatus status = BLE_STATUS_INVALID_PARAMS;
	  //uint8_t crc_result;

		/*Data Packet to send to remote*/
		//Notification_Data_Buffer[0] += 1;
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		sprintf(uartBuffer, "bz_rx_app: SendData\n\r");
		//HAL_UART_Transmit(&huart1, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
		/* compute CRC */
		//crc_result = APP_BLE_ComputeCRC8((uint8_t*) Notification_Data_Buffer, (MTUSizeValue - 1));
		//Notification_Data_Buffer[MTUSizeValue - 1] = crc_result;
		uint8_t chr = 0x30;
		for (int iii = 0; iii < MTUSizeValue; iii++) {
			if (chr > 0x39) {
				chr = 0x30;
			}
			Notification_Data_Buffer[iii] = chr++;
		}
		BZ_Context.TxData.p_Payload = Notification_Data_Buffer;
		BZ_Context.TxData.Length =  MTUSizeValue;

		status = BLUZ_UpdateValue(BLUZ_RX, (BLUZ_Data_t *) &BZ_Context.TxData);

		if (status == BLE_STATUS_INSUFFICIENT_RESOURCES)
		{
			bzero((char *) uartBuffer, sizeof(uartBuffer));
			sprintf(uartBuffer, "bz_rx_app: Error transfer with status: %d\n\r", status);
			//HAL_UART_Transmit(&huart1, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
		}
		else
		{
			bzero((char *) uartBuffer, sizeof(uartBuffer));
			sprintf(uartBuffer, "bz_rx_app: Send complete\n\r");
			//HAL_UART_Transmit(&huart1, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
		}

	  BleStackCB_Process();
	}
  return;
}

/* USER CODE END FD_LOCAL_FUNCTIONS*/
