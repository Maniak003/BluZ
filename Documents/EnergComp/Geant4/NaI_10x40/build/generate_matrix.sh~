#!/bin/bash
# generate_response_matrix.sh
source ~/tmp/Geant4-11.4.1-Linux/bin/geant4.sh
export G4FORCENUMBEROFTHREADS=1

#30 33 35 40 50 59.5 88 100 122 150 165 200 245 300 356 400 450 500 511 600 662 700 800 835 900 1000 1173 1200 1332 1500 1800 2000 2614 3000 

ENERGIES=(30 33 35 40 50 59.5 88 100 122 150 165 200 245 300 356 400 450 500 511 600 662 700 800 835 900 1000 1173 1200 1332 1500 1800 2000 2614 3000)

for E in "${ENERGIES[@]}"; do
    #echo "=== Simulating ${E} keV ==="
    
    # Создаём временный макрос
    cat > run_temp.mac << EOF
/run/initialize
/gun/particle gamma
/gun/energy ${E} keV
/gun/position 0 0 -5 cm
/gun/direction 0 0 1
/run/beamOn 100000
EOF
    
    ./exampleB1 run_temp.mac > /dev/null 2>&1
    mv NaI_response_h1_Edep.csv "response_${E}keV.csv"
    echo "Saved: response_${E}keV.csv"
done

rm -f run_temp.mac
echo "=== Matrix generation complete ==="

