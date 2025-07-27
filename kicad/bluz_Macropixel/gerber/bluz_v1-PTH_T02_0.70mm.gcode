; Produced by drl2gcode.py originally by Franco Lanza

; select absolute coordinate system
G90
; metric
G21
; G61 exact path mode was requested but not implemented
; start spindle
M3 S10000.0


; T2 Diameter: 0.70mm
G1 F1200.0 Z4.0
G1 F1200.0 X0.0 Y0.0
G1 F1200.0 Z0.2
G1 F100.0 Z-3.0
G1 F1200.0 Z4.0
G1 F1200.0 X2.5 Y-17.44
G1 F1200.0 Z0.2
G1 F100.0 Z-3.0
G1 F1200.0 Z4.0
G1 F1200.0 X27.38 Y-17.43
G1 F1200.0 Z0.2
G1 F100.0 Z-3.0
G1 F1200.0 Z4.0
G1 F1200.0 X29.86 Y0.0
G1 F1200.0 Z0.2
G1 F100.0 Z-3.0

; stop spindle
M5
; go to safe height
G1 Z4.0 F30000
; program ends
M2
