/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    service1.c
  * @author  MCD Application Team
  * @brief   service1 definition.
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
#include "common_blesvc.h"
#include "bluz.h"

/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/

/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

typedef struct{
  uint16_t  BluzSvcHdle;                  /**< Bluz Service Handle */
  uint16_t  RxCharHdle;                  /**< RX Characteristic Handle */
  uint16_t  TxCharHdle;                  /**< TX Characteristic Handle */
/* USER CODE BEGIN Context */
  /* Place holder for Characteristic Descriptors Handle*/

/* USER CODE END Context */
}BLUZ_Context_t;

/* Private defines -----------------------------------------------------------*/
#define UUID_128_SUPPORTED  1

#if (UUID_128_SUPPORTED == 1)
#define BM_UUID_LENGTH  UUID_TYPE_128
#else
#define BM_UUID_LENGTH  UUID_TYPE_16
#endif

#define BM_REQ_CHAR_SIZE    (3)

/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* External variables --------------------------------------------------------*/
/* USER CODE BEGIN EV */

/* USER CODE END EV */

/* Private macros ------------------------------------------------------------*/
#define CHARACTERISTIC_DESCRIPTOR_ATTRIBUTE_OFFSET         2
#define CHARACTERISTIC_VALUE_ATTRIBUTE_OFFSET              1
/* USER CODE BEGIN PM */
uint16_t MTUSizeValue;
/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
static const uint16_t SizeRx = 255;
static const uint16_t SizeTx = 255;

static BLUZ_Context_t BLUZ_Context;

/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
static SVCCTL_EvtAckStatus_t BLUZ_EventHandler(void *p_pckt);

/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Functions Definition ------------------------------------------------------*/
/* USER CODE BEGIN PFD */

/* USER CODE END PFD */

/* Private functions ----------------------------------------------------------*/

#define COPY_UUID_128(uuid_struct, uuid_15, uuid_14, uuid_13, uuid_12, uuid_11, uuid_10, uuid_9, uuid_8, uuid_7, uuid_6, uuid_5, uuid_4, uuid_3, uuid_2, uuid_1, uuid_0) \
do {\
    uuid_struct[0] = uuid_0; uuid_struct[1] = uuid_1; uuid_struct[2] = uuid_2; uuid_struct[3] = uuid_3; \
    uuid_struct[4] = uuid_4; uuid_struct[5] = uuid_5; uuid_struct[6] = uuid_6; uuid_struct[7] = uuid_7; \
    uuid_struct[8] = uuid_8; uuid_struct[9] = uuid_9; uuid_struct[10] = uuid_10; uuid_struct[11] = uuid_11; \
    uuid_struct[12] = uuid_12; uuid_struct[13] = uuid_13; uuid_struct[14] = uuid_14; uuid_struct[15] = uuid_15; \
}while(0)

/*
 The following 128bits UUIDs have been generated from the random UUID
 generator:
 0000FE80CC7A482A984A7F2ED5B3E58F: Service 128bits UUID
 0000FE818E2245419D4C21EDAE82ED19: Characteristic 128bits UUID
 0000FE828E2245419D4C21EDAE82ED19: Characteristic 128bits UUID
 */
#define COPY_BLUZ_UUID(uuid_struct)       COPY_UUID_128(uuid_struct,0x00,0x00,0xfe,0x80,0xcc,0x7a,0x48,0x2a,0x98,0x4a,0x7f,0x2e,0xd5,0xb3,0xe5,0x8f)
#define COPY_RX_UUID(uuid_struct)       COPY_UUID_128(uuid_struct,0x00,0x00,0xfe,0x81,0x8e,0x22,0x45,0x41,0x9d,0x4c,0x21,0xed,0xae,0x82,0xed,0x19)
#define COPY_TX_UUID(uuid_struct)       COPY_UUID_128(uuid_struct,0x00,0x00,0xfe,0x82,0x8e,0x22,0x45,0x41,0x9d,0x4c,0x21,0xed,0xae,0x82,0xed,0x19)

/* USER CODE BEGIN PF */

/* USER CODE END PF */

/**
 * @brief  Event handler
 * @param  p_Event: Address of the buffer holding the p_Event
 * @retval Ack: Return whether the p_Event has been managed or not
 */
static SVCCTL_EvtAckStatus_t BLUZ_EventHandler(void *p_Event)
{
  SVCCTL_EvtAckStatus_t return_value;
  hci_event_pckt *p_event_pckt;
  evt_blecore_aci *p_blecore_evt;
  aci_gatt_attribute_modified_event_rp0 *p_attribute_modified;
  BLUZ_NotificationEvt_t                 notification;
  /* USER CODE BEGIN Service1_EventHandler_1 */

  /* USER CODE END Service1_EventHandler_1 */

  return_value = SVCCTL_EvtNotAck;
  p_event_pckt = (hci_event_pckt *)(((hci_uart_pckt*)p_Event)->data);

  switch(p_event_pckt->evt)
  {
    case HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE:
      p_blecore_evt = (evt_blecore_aci*)p_event_pckt->data;
      switch(p_blecore_evt->ecode)
      {
        case ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE:
        {
          /* USER CODE BEGIN EVT_BLUE_GATT_ATTRIBUTE_MODIFIED_BEGIN */
            bzero((char *) uartBuffer, sizeof(uartBuffer));
            sprintf(uartBuffer, "ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE\n\r");
            HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

          /* USER CODE END EVT_BLUE_GATT_ATTRIBUTE_MODIFIED_BEGIN */
          p_attribute_modified = (aci_gatt_attribute_modified_event_rp0*)p_blecore_evt->data;
          notification.ConnectionHandle         = p_attribute_modified->Connection_Handle;
          notification.AttributeHandle          = p_attribute_modified->Attr_Handle;
          notification.DataTransfered.Length    = p_attribute_modified->Attr_Data_Length;
          notification.DataTransfered.p_Payload = p_attribute_modified->Attr_Data;
          if(p_attribute_modified->Attr_Handle == (BLUZ_Context.RxCharHdle + CHARACTERISTIC_DESCRIPTOR_ATTRIBUTE_OFFSET))
          {
            return_value = SVCCTL_EvtAckFlowEnable;
            /* USER CODE BEGIN Service1_Char_1 */

            /* USER CODE END Service1_Char_1 */
            switch(p_attribute_modified->Attr_Data[0])
            {
              /* USER CODE BEGIN Service1_Char_1_attribute_modified */
            /*
				bzero((char *) uartBuffer, sizeof(uartBuffer));
				sprintf(uartBuffer, "Service1_Char_1_attribute_modified\n\r");
				HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100); */

              /* USER CODE END Service1_Char_1_attribute_modified */

              /* Disabled Notification management */
              case (!(COMSVC_Notification)):
                /* USER CODE BEGIN Service1_Char_1_Disabled_BEGIN */
				bzero((char *) uartBuffer, sizeof(uartBuffer));
				sprintf(uartBuffer, "Service1_Char_1_Disabled_BEGIN\n\r");
				HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

                /* USER CODE END Service1_Char_1_Disabled_BEGIN */
                notification.EvtOpcode = BLUZ_RX_NOTIFY_DISABLED_EVT;
                BLUZ_Notification(&notification);
                /* USER CODE BEGIN Service1_Char_1_Disabled_END */

                /* USER CODE END Service1_Char_1_Disabled_END */
                break;

              /* Enabled Notification management */
              case COMSVC_Notification:
                /* USER CODE BEGIN Service1_Char_1_COMSVC_Notification_BEGIN */
  				bzero((char *) uartBuffer, sizeof(uartBuffer));
  				sprintf(uartBuffer, "Service1_Char_1_COMSVC_Notification_BEGIN\n\r");
  				HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

                /* USER CODE END Service1_Char_1_COMSVC_Notification_BEGIN */
                notification.EvtOpcode = BLUZ_RX_NOTIFY_ENABLED_EVT;
                BLUZ_Notification(&notification);
                /* USER CODE BEGIN Service1_Char_1_COMSVC_Notification_END */

                /* USER CODE END Service1_Char_1_COMSVC_Notification_END */
                break;

              default:
                /* USER CODE BEGIN Service1_Char_1_default */
    				bzero((char *) uartBuffer, sizeof(uartBuffer));
    				sprintf(uartBuffer, "Service1_Char_1_default\n\r");
    				HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

                /* USER CODE END Service1_Char_1_default */
                break;
            }
          }  /* if(p_attribute_modified->Attr_Handle == (BLUZ_Context.RxCharHdle + CHARACTERISTIC_DESCRIPTOR_ATTRIBUTE_OFFSET))*/

          else if(p_attribute_modified->Attr_Handle == (BLUZ_Context.TxCharHdle + CHARACTERISTIC_DESCRIPTOR_ATTRIBUTE_OFFSET))
          {
            return_value = SVCCTL_EvtAckFlowEnable;
            /* USER CODE BEGIN Service1_Char_2 */

            /* USER CODE END Service1_Char_2 */
            switch(p_attribute_modified->Attr_Data[0])
            {
              /* USER CODE BEGIN Service1_Char_2_attribute_modified */

              /* USER CODE END Service1_Char_2_attribute_modified */

              /* Disabled Notification management */
              case (!(COMSVC_Notification)):
                /* USER CODE BEGIN Service1_Char_2_Disabled_BEGIN */

                /* USER CODE END Service1_Char_2_Disabled_BEGIN */
                notification.EvtOpcode = BLUZ_TX_NOTIFY_DISABLED_EVT;
                BLUZ_Notification(&notification);
                /* USER CODE BEGIN Service1_Char_2_Disabled_END */

                /* USER CODE END Service1_Char_2_Disabled_END */
                break;

              /* Enabled Notification management */
              case COMSVC_Notification:
                /* USER CODE BEGIN Service1_Char_2_COMSVC_Notification_BEGIN */

                /* USER CODE END Service1_Char_2_COMSVC_Notification_BEGIN */
                notification.EvtOpcode = BLUZ_TX_NOTIFY_ENABLED_EVT;
                BLUZ_Notification(&notification);
                /* USER CODE BEGIN Service1_Char_2_COMSVC_Notification_END */

                /* USER CODE END Service1_Char_2_COMSVC_Notification_END */
                break;

              default:
                /* USER CODE BEGIN Service1_Char_2_default */

                /* USER CODE END Service1_Char_2_default */
                break;
            }
          }  /* if(p_attribute_modified->Attr_Handle == (BLUZ_Context.TxCharHdle + CHARACTERISTIC_DESCRIPTOR_ATTRIBUTE_OFFSET))*/

          else if(p_attribute_modified->Attr_Handle == (BLUZ_Context.TxCharHdle + CHARACTERISTIC_VALUE_ATTRIBUTE_OFFSET))
          {
            return_value = SVCCTL_EvtAckFlowEnable;

            notification.EvtOpcode = BLUZ_TX_WRITE_NO_RESP_EVT;
            /* USER CODE BEGIN Service1_Char_2_ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE */

            /* USER CODE END Service1_Char_2_ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE */
            BLUZ_Notification(&notification);
          } /* if(p_attribute_modified->Attr_Handle == (BLUZ_Context.TxCharHdle + CHARACTERISTIC_VALUE_ATTRIBUTE_OFFSET))*/

          /* USER CODE BEGIN EVT_BLUE_GATT_ATTRIBUTE_MODIFIED_END */

          /* USER CODE END EVT_BLUE_GATT_ATTRIBUTE_MODIFIED_END */
          break;/* ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE */
        }
        case ACI_GATT_READ_PERMIT_REQ_VSEVT_CODE :
        {
          /* USER CODE BEGIN EVT_BLUE_GATT_READ_PERMIT_REQ_BEGIN */

          /* USER CODE END EVT_BLUE_GATT_READ_PERMIT_REQ_BEGIN */

          /* USER CODE BEGIN EVT_BLUE_GATT_READ_PERMIT_REQ_END */

          /* USER CODE END EVT_BLUE_GATT_READ_PERMIT_REQ_END */
          break;/* ACI_GATT_READ_PERMIT_REQ_VSEVT_CODE */
        }
        case ACI_GATT_WRITE_PERMIT_REQ_VSEVT_CODE:
        {
          /* USER CODE BEGIN EVT_BLUE_GATT_WRITE_PERMIT_REQ_BEGIN */

          /* USER CODE END EVT_BLUE_GATT_WRITE_PERMIT_REQ_BEGIN */

          /* USER CODE BEGIN EVT_BLUE_GATT_WRITE_PERMIT_REQ_END */

          /* USER CODE END EVT_BLUE_GATT_WRITE_PERMIT_REQ_END */
          break;/* ACI_GATT_WRITE_PERMIT_REQ_VSEVT_CODE */
        }
        case ACI_GATT_TX_POOL_AVAILABLE_VSEVT_CODE:
        {
          aci_gatt_tx_pool_available_event_rp0 *p_tx_pool_available_event;
          p_tx_pool_available_event = (aci_gatt_tx_pool_available_event_rp0 *) p_blecore_evt->data;
          UNUSED(p_tx_pool_available_event);

          /* USER CODE BEGIN ACI_GATT_TX_POOL_AVAILABLE_VSEVT_CODE */

          /* USER CODE END ACI_GATT_TX_POOL_AVAILABLE_VSEVT_CODE */
          break;/* ACI_GATT_TX_POOL_AVAILABLE_VSEVT_CODE*/
        }
        case ACI_ATT_EXCHANGE_MTU_RESP_VSEVT_CODE:
        {
          aci_att_exchange_mtu_resp_event_rp0 *p_exchange_mtu;
          p_exchange_mtu = (aci_att_exchange_mtu_resp_event_rp0 *)  p_blecore_evt->data;
          UNUSED(p_exchange_mtu);

          /* USER CODE BEGIN ACI_ATT_EXCHANGE_MTU_RESP_VSEVT_CODE */
          bzero((char *) uartBuffer, sizeof(uartBuffer));
          sprintf(uartBuffer, "ACI_ATT_EXCHANGE_MTU_RESP_VSEVT_CODE\n\r");
          HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
          if (p_exchange_mtu->Server_RX_MTU < DATA_NOTIFICATION_MAX_PACKET_SIZE)
          {
            MTUSizeValue = p_exchange_mtu->Server_RX_MTU - 3;
          }
          else
          {
            MTUSizeValue = DATA_NOTIFICATION_MAX_PACKET_SIZE;
          }

          /* USER CODE END ACI_ATT_EXCHANGE_MTU_RESP_VSEVT_CODE */
          break;/* ACI_ATT_EXCHANGE_MTU_RESP_VSEVT_CODE */
        }
        /* USER CODE BEGIN BLECORE_EVT */

        /* USER CODE END BLECORE_EVT */
        default:
          /* USER CODE BEGIN EVT_DEFAULT */

          /* USER CODE END EVT_DEFAULT */
          break;
      }
      /* USER CODE BEGIN EVT_VENDOR*/

      /* USER CODE END EVT_VENDOR*/
      break; /* HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE */

      /* USER CODE BEGIN EVENT_PCKT_CASES*/

      /* USER CODE END EVENT_PCKT_CASES*/

    default:
      /* USER CODE BEGIN EVENT_PCKT*/

      /* USER CODE END EVENT_PCKT*/
      break;
  }

  /* USER CODE BEGIN Service1_EventHandler_2 */

  /* USER CODE END Service1_EventHandler_2 */

  return(return_value);
}/* end BLUZ_EventHandler */

/* Public functions ----------------------------------------------------------*/

/**
 * @brief  Service initialization
 * @param  None
 * @retval None
 */
void BLUZ_Init(void)
{
  Char_UUID_t  uuid;
  tBleStatus ret;
  uint8_t max_attr_record;

  /* USER CODE BEGIN SVCCTL_InitService1Svc_1 */

  /* USER CODE END SVCCTL_InitService1Svc_1 */

  /**
   *  Register the event handler to the BLE controller
   */
  SVCCTL_RegisterSvcHandler(BLUZ_EventHandler);

  /**
   * BluZ
   *
   * Max_Attribute_Records = 1 + 2*2 + 1*no_of_char_with_notify_or_indicate_property + 1*no_of_char_with_broadcast_property
   * service_max_attribute_record = 1 for BluZ +
   *                                2 for RX +
   *                                2 for TX +
   *                                1 for RX configuration descriptor +
   *                                1 for TX configuration descriptor +
   *                              = 7
   * This value doesn't take into account number of descriptors manually added
   * In case of descriptors added, please update the max_attr_record value accordingly in the next SVCCTL_InitService User Section
   */
  max_attr_record = 7;

  /* USER CODE BEGIN SVCCTL_InitService */
  /* max_attr_record to be updated if descriptors have been added */

  /* USER CODE END SVCCTL_InitService */

  COPY_BLUZ_UUID(uuid.Char_UUID_128);
  ret = aci_gatt_add_service(UUID_TYPE_128,
                             (Service_UUID_t *) &uuid,
                             PRIMARY_SERVICE,
                             max_attr_record,
                             &(BLUZ_Context.BluzSvcHdle));
  if (ret != BLE_STATUS_SUCCESS)
  {
    LOG_INFO_APP("  Fail   : aci_gatt_add_service command: BluZ, error code: 0x%x \n\r", ret);
  }
  else
  {
    LOG_INFO_APP("  Success: aci_gatt_add_service command: BluZ \n\r");
  }

  /**
   * RX
   */
  COPY_RX_UUID(uuid.Char_UUID_128);
  ret = aci_gatt_add_char(BLUZ_Context.BluzSvcHdle,
                          UUID_TYPE_128,
                          (Char_UUID_t *) &uuid,
                          SizeRx,
                          CHAR_PROP_NOTIFY,
                          ATTR_PERMISSION_NONE,
                          GATT_NOTIFY_ATTRIBUTE_WRITE,
                          0x10,
                          CHAR_VALUE_LEN_CONSTANT,
                          &(BLUZ_Context.RxCharHdle));
  if (ret != BLE_STATUS_SUCCESS)
  {
    LOG_INFO_APP("  Fail   : aci_gatt_add_char command   : RX, error code: 0x%2X\n", ret);
  }
  else
  {
    LOG_INFO_APP("  Success: aci_gatt_add_char command   : RX\n");
  }

  /* USER CODE BEGIN SVCCTL_InitService1Char1 */
  /* Place holder for Characteristic Descriptors */

  /* USER CODE END SVCCTL_InitService1Char1 */

  /**
   * TX
   */
  COPY_TX_UUID(uuid.Char_UUID_128);
  ret = aci_gatt_add_char(BLUZ_Context.BluzSvcHdle,
                          UUID_TYPE_128,
                          (Char_UUID_t *) &uuid,
                          SizeTx,
                          CHAR_PROP_WRITE_WITHOUT_RESP | CHAR_PROP_NOTIFY,
                          ATTR_PERMISSION_NONE,
                          GATT_NOTIFY_ATTRIBUTE_WRITE,
                          0x10,
                          CHAR_VALUE_LEN_CONSTANT,
                          &(BLUZ_Context.TxCharHdle));
  if (ret != BLE_STATUS_SUCCESS)
  {
    LOG_INFO_APP("  Fail   : aci_gatt_add_char command   : TX, error code: 0x%2X\n", ret);
  }
  else
  {
    LOG_INFO_APP("  Success: aci_gatt_add_char command   : TX\n");
  }

  /* USER CODE BEGIN SVCCTL_InitService1Char2 */
  /* Place holder for Characteristic Descriptors */

  /* USER CODE END SVCCTL_InitService1Char2 */

  /* USER CODE BEGIN SVCCTL_InitService1Svc_2 */

  /* USER CODE END SVCCTL_InitService1Svc_2 */

  return;
}

/**
 * @brief  Characteristic update
 * @param  CharOpcode: Characteristic identifier
 * @param  Service_Instance: Instance of the service to which the characteristic belongs
 *
 */
tBleStatus BLUZ_UpdateValue(BLUZ_CharOpcode_t CharOpcode, BLUZ_Data_t *pData)
{
  tBleStatus ret = BLE_STATUS_INVALID_PARAMS;
  /* USER CODE BEGIN Service1_App_Update_Char_1 */
	//bzero((char *) uartBuffer, sizeof(uartBuffer));
	//sprintf(uartBuffer, "Service1_App_Update_Char_1\n\rLength: %d\n\r", pData->Length);
	//HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

  /* USER CODE END Service1_App_Update_Char_1 */

  switch(CharOpcode)
  {
    case BLUZ_RX:
      ret = aci_gatt_update_char_value(BLUZ_Context.BluzSvcHdle,
                                       BLUZ_Context.RxCharHdle,
                                       0, /* charValOffset */
                                       pData->Length, /* charValueLen */
                                       (uint8_t *)pData->p_Payload);
      if (ret != BLE_STATUS_SUCCESS)
      {
        LOG_INFO_APP("  Fail   : aci_gatt_update_char_value RX command, error code: 0x%2X\n", ret);
      }
      else
      {
        LOG_INFO_APP("  Success: aci_gatt_update_char_value RX command\n");
      }
      /* USER CODE BEGIN Service1_Char_Value_1*/
		//bzero((char *) uartBuffer, sizeof(uartBuffer));
		//sprintf(uartBuffer, "Service1_Char_Value_1\n\r");
		//HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		if (ret != BLE_STATUS_SUCCESS) {
			sprintf(uartBuffer, "NO SUCCESS\n\r");
		} else {
			sprintf(uartBuffer, "SUCCESS\n\r");
		}
		//HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		/*
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		sprintf(uartBuffer, "DATA [%d]: \n\r", pData->Length);
		HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, sizeof(uartBuffer), 100);
		for (int iii = 0; iii < pData->Length; iii++) {
			sprintf(uartBuffer, "%02X ", pData->p_Payload[iii]);
			HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, 3, 100);
		}
		HAL_UART_Transmit(&huart2, (uint8_t *) "\n\r", 2, 100);
		*/
      /* USER CODE END Service1_Char_Value_1*/
      break;

    case BLUZ_TX:
      ret = aci_gatt_update_char_value(BLUZ_Context.BluzSvcHdle,
                                       BLUZ_Context.TxCharHdle,
                                       0, /* charValOffset */
                                       pData->Length, /* charValueLen */
                                       (uint8_t *)pData->p_Payload);
      if (ret != BLE_STATUS_SUCCESS)
      {
        LOG_INFO_APP("  Fail   : aci_gatt_update_char_value TX command, error code: 0x%2X\n", ret);
      }
      else
      {
        LOG_INFO_APP("  Success: aci_gatt_update_char_value TX command\n");
      }
      /* USER CODE BEGIN Service1_Char_Value_2*/
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		sprintf(uartBuffer, "Service1_Char_Value_2\n\r");
		HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
      /* USER CODE END Service1_Char_Value_2*/
      break;

    default:
      break;
  }

  /* USER CODE BEGIN Service1_App_Update_Char_2 */

  /* USER CODE END Service1_App_Update_Char_2 */

  return ret;
}
