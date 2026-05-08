//
// ********************************************************************
// * License and Disclaimer                                           *
// *                                                                  *
// * The  Geant4 software  is  copyright of the Copyright Holders  of *
// * the Geant4 Collaboration.  It is provided  under  the terms  and *
// * conditions of the Geant4 Software License,  included in the file *
// * LICENSE and available at  http://cern.ch/geant4/license .  These *
// * include a list of copyright holders.                             *
// *                                                                  *
// * Neither the authors of this software system, nor their employing *
// * institutes,nor the agencies providing financial support for this *
// * work  make  any representation or  warranty, express or implied, *
// * regarding  this  software system or assume any liability for its *
// * use.  Please see the license in the file  LICENSE  and URL above *
// * for the full disclaimer and the limitation of liability.         *
// *                                                                  *
// * This  code  implementation is the result of  the  scientific and *
// * technical work of the GEANT4 collaboration.                      *
// * By using,  copying,  modifying or  distributing the software (or *
// * any work based  on the software)  you  agree  to acknowledge its *
// * use  in  resulting  scientific  publications,  and indicate your *
// * acceptance of all terms of the Geant4 Software license.          *
// ********************************************************************
//
/// \file DetectorConstruction.cc
/// \brief Implementation of the B1::DetectorConstruction class

#include "DetectorConstruction.hh"

#include "G4Box.hh"
#include "G4Tubs.hh"              // <-- ДОБАВЛЕНО: для цилиндра
#include "G4LogicalVolume.hh"
#include "G4NistManager.hh"
#include "G4PVPlacement.hh"
#include "G4SystemOfUnits.hh"

namespace B1
{

//....oooOO0OOooo........oooOO0OOooo........oooOO0OOooo........oooOO0OOooo......

G4VPhysicalVolume* DetectorConstruction::Construct()
{
  G4NistManager* nist = G4NistManager::Instance();
  G4bool checkOverlaps = true;

  // === World ===
  G4double world_sizeXY = 30 * cm;
  G4double world_sizeZ  = 30 * cm;
  G4Material* world_mat = nist->FindOrBuildMaterial("G4_AIR");

  auto solidWorld = new G4Box("World", 0.5*world_sizeXY, 0.5*world_sizeXY, 0.5*world_sizeZ);
  auto logicWorld = new G4LogicalVolume(solidWorld, world_mat, "World");
  auto physWorld = new G4PVPlacement(nullptr, G4ThreeVector(), logicWorld, "World", nullptr, false, 0, checkOverlaps);

  // === NaI(Tl) Crystal: цилиндр, диаметр 10 мм, длина 10 мм ===
  G4Material* NaI = nist->FindOrBuildMaterial("G4_SODIUM_IODIDE");
  if (!NaI) {
    G4Exception("DetectorConstruction", "MatNotFound", FatalException,
                "Material G4_SODIUM_IODIDE not found!");
  }

  G4double crystalR = 5.0*mm;   // радиус = диаметр/2
  G4double crystalL = 10.0*mm;  // длина
  
  auto solidCrystal = new G4Tubs("NaICrystal", 0, crystalR, crystalL/2, 0., 360.*deg);
  auto logicCrystal = new G4LogicalVolume(solidCrystal, NaI, "CrystalLog");
  
  new G4PVPlacement(nullptr, G4ThreeVector(0,0,0), logicCrystal, "NaICrystal", logicWorld, false, 0, checkOverlaps);

  // === КРИТИЧНО: установить scoring volume на кристалл ===
  fScoringVolume = logicCrystal;

  return physWorld;
}

//....oooOO0OOooo........oooOO0OOooo........oooOO0OOooo........oooOO0OOooo......

}  // namespace B1
