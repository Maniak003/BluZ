#!/bin/bash
# generate_matrix.sh - Генерация матрицы отклика для NaI(Tl) 10×10 мм

source ~/tmp/Geant4-11.4.1-Linux/bin/geant4.sh
export G4FORCENUMBEROFTHREADS=1

# === Чтение config.txt ===
if [[ ! -f config.txt ]]; then
    echo "Ошибка: файл config.txt не найден!"
    exit 1
fi

while IFS='=' read -r key value; do
    [[ "$key" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$key" ]] && continue
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    case "$key" in
        ENERGIES) IFS=' ' read -r -a ENERGIES <<< "$value" ;;
        N_EVENTS) N_EVENTS="$value" ;;
        OUTPUT_FILE) OUTPUT_FILE="$value" ;;
    esac
done < config.txt

echo "=== Запуск: ${#ENERGIES[@]} энергий × $N_EVENTS событий ==="
START_TIME=$(date +%s)

for E in "${ENERGIES[@]}"; do
    E_NAME=$(echo "$E" | sed 's/\./_/g')
    cat > run_temp.mac << EOF
/run/initialize
/gun/particle gamma
/gun/energy ${E} keV
/gun/position 0 0 -5 cm
/gun/direction 0 0 1
/run/beamOn ${N_EVENTS}
EOF
    
    echo -n "[$(date +%H:%M:%S)] ${E} keV ... "
    ./exampleB1 run_temp.mac > /dev/null 2>&1 &
    mv NaI_response_h1_Edep.csv "response_${E_NAME}keV.csv"
    echo "Ok"
done

rm -f run_temp.mac
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "=== Готово за $((DURATION/60)) мин $((DURATION%60)) с ==="
