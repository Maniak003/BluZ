/*
 * main_display.h
 *
 *  Created on: Oct 18, 2026
 *      Author: ed
 */

#ifndef INC_MAIN_DISPLAY_H_
#define INC_MAIN_DISPLAY_H_

#include "main.h"

#define CPS_Y_POSITION 15
#define CPS_X_POSITION 30
#define ACTIVITY_Y 80
#define ACTIVITY_H 8
#define ACTIVITY_SCALE 10
#define LEVEL_Y 95
#define LEVEL_H 8
#define HEIGHT_GISTOGRAM 30
#define DOSIMETER_WIN 20
#define DISPLAY_REFRASH_INTERVAL 1000

extern unsigned char lcd_str_buffer[40];
extern SharpLCD_t lcd;

void lcd_init(void);
void redraw_display(void);
void draw_activity(void);
void draw_dosimeter(void);
void draw_indicator(void);
void draw_gistogram_dosimeter(void);


#endif /* INC_MAIN_DISPLAY_H_ */
