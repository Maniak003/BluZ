clear, clc, close all;
window_size = 3; % Размер окна

A = importdata('fon.dat');
B = sma(A, window_size);
subplot(2,1,1)
plot(A);
subplot(2,1,2) 
plot(B);
