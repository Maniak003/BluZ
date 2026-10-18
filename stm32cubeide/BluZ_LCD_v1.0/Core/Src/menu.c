/*
 * menu.c
 *
 *  Created on: Oct 18, 2026
 *      Author: ed
 */

#include "menu.h"

bool menu_active_flag = false, submenu_active_flag = false;

char * menu_text[] = {
		"Exit",
		"Sound click",
		"LED click",
		"Level1",
		"Sound level1",
		"Vibro level1",
		"Level2",
		"Sound level2",
		"Vibro level2",
		"Sleep time",
		"Flash click",
};

/* Текущие значения настроек */
uint8_t menu_items[] = {
		0,			// 00 Exit
		0,			// 01 Sound click
		0,			// 02 LED click
		0,			// 03 Level1
		0,			// 04 SoundLevel 1
		0,			// 05 VibroLevel 1
		0,			// 06 Level 2
		0,			// 07 SoundLevel 2
		0,			// 08 VibroLevel 2
		0,			// 09 SleepTime
		0,			// 10 Flash click
};

/* Типы значений */
uint8_t menu_types[] = {
		EXIT_TYPE,			// Выход
		BOOLEAN_TYPE,		// Флаг
		BOOLEAN_TYPE,		// Флаг
		NUMERIC_TYPE,		// Цифровое
		BOOLEAN_TYPE,		// Флаг
		BOOLEAN_TYPE,		// Флаг
		NUMERIC_TYPE,		// Цифровое
		BOOLEAN_TYPE,		// Флаг
		BOOLEAN_TYPE,		// Флаг
		NUMERIC_TYPE,		// Цифровое
		BOOLEAN_TYPE,		// Флаг
};
