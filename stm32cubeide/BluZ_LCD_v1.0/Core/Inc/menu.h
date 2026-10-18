/*
 * manu.h
 *
 *  Created on: Oct 18, 2026
 *      Author: ed
 */

#ifndef INC_MENU_H_
#define INC_MENU_H_

#include "main.h"

extern uint8_t menu_types[];
extern uint8_t menu_items[];
extern char * menu_text[];
extern bool menu_active_flag;
extern bool submenu_active_flag;

enum BTN_STAT {
	BTN_UP,
	BTN_DOWN,
	BTN_MENU
};

enum {
	EXIT_TYPE,
	BOOLEAN_TYPE,
	NUMERIC_TYPE
};

enum {
	EXIT_ITEM,
	SOUND_CLICK_ITEM,
	LED_CLICK_ITEM,
	LEVEL1_ITEM,
	SOUND_LEVEL1_ITEM,
	VIBRO_LEVEL1_ITEM,
	LEVEL2_ITEM,
	SOUND_LEVEL2_ITEM,
	VIBRO_LEVEL2_ITEM,
	SLEEP_TIME_ITEM,
	FLASH_CLICK_ITEM,
};

enum {
	SOUND_CLICK_ITEM_BIT,
	LED_CLICK_ITEM_BIT,
	SOUND_LEVEL1_ITEM_BIT,
	VIBRO_LEVEL1_ITEM_BIT,
	SOUND_LEVEL2_ITEM_BIT,
	VIBRO_LEVEL2_ITEM_BIT,
	FLASH_CLICK_ITEM_BIT,
};


#endif /* INC_MENU_H_ */
