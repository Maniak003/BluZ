/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    service1.h
  * @author  MCD Application Team
  * @brief   Header for service1.c
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

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef BLUZ_H
#define BLUZ_H

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#ifndef __INCLUDE_MAIN_H
#define __INCLUDE_MAIN_H
#include "main.h"
#endif
/* USER CODE END Includes */

/* Exported defines ----------------------------------------------------------*/
/* USER CODE BEGIN ED */

/* USER CODE END ED */

/* Exported types ------------------------------------------------------------*/
typedef enum
{
  BLUZ_RX,
  BLUZ_TX,
  /* USER CODE BEGIN Service1_CharOpcode_t */

  /* USER CODE END Service1_CharOpcode_t */
  BLUZ_CHAROPCODE_LAST
} BLUZ_CharOpcode_t;

typedef enum
{
  BLUZ_RX_NOTIFY_ENABLED_EVT,
  BLUZ_RX_NOTIFY_DISABLED_EVT,
  BLUZ_TX_WRITE_NO_RESP_EVT,
  BLUZ_TX_NOTIFY_ENABLED_EVT,
  BLUZ_TX_NOTIFY_DISABLED_EVT,
  /* USER CODE BEGIN Service1_OpcodeEvt_t */

  /* USER CODE END Service1_OpcodeEvt_t */
  BLUZ_BOOT_REQUEST_EVT
} BLUZ_OpcodeEvt_t;

typedef struct
{
  uint8_t *p_Payload;
  uint8_t Length;

  /* USER CODE BEGIN Service1_Data_t */

  /* USER CODE END Service1_Data_t */
} BLUZ_Data_t;

typedef struct
{
  BLUZ_OpcodeEvt_t       EvtOpcode;
  BLUZ_Data_t             DataTransfered;
  uint16_t                ConnectionHandle;
  uint16_t                AttributeHandle;
  uint8_t                 ServiceInstance;
  /* USER CODE BEGIN Service1_NotificationEvt_t */

  /* USER CODE END Service1_NotificationEvt_t */
} BLUZ_NotificationEvt_t;

/* USER CODE BEGIN ET */

/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/
/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* External variables --------------------------------------------------------*/
/* USER CODE BEGIN EV */

extern char uartBuffer[400];
extern UART_HandleTypeDef huart2;
/* USER CODE END EV */

/* Exported macros -----------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

/* Exported functions prototypes ---------------------------------------------*/
void BLUZ_Init(void);
void BLUZ_Notification(BLUZ_NotificationEvt_t *p_Notification);
tBleStatus BLUZ_UpdateValue(BLUZ_CharOpcode_t CharOpcode, BLUZ_Data_t *pData);
/* USER CODE BEGIN EFP */

/* USER CODE END EFP */

#ifdef __cplusplus
}
#endif

#endif /*BLUZ_H */
