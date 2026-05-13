// ... [лицензия без изменений] ...
/// \file EventAction.cc
/// \brief Implementation of the B1::EventAction class

#include "EventAction.hh"
#include "RunAction.hh"

// === ДОБАВИТЬ эти include ===
#include "G4AnalysisManager.hh"
//#include "G4RandGauss.hh"
#include "Randomize.hh"
#include <cmath>

namespace B1
{
//....oooOO0OOooo........oooOO0OOooo........oooOO0OOooo........oooOO0OOooo......

EventAction::EventAction(RunAction* runAction) : fRunAction(runAction) {}

//....oooOO0OOooo........oooOO0OOooo........oooOO0OOooo........oooOO0OOooo......

void EventAction::BeginOfEventAction(const G4Event*)
{
  fEdep = 0.;
}

//....oooOO0OOooo........oooOO0OOooo........oooOO0OOooo........oooOO0OOooo......

void EventAction::EndOfEventAction(const G4Event*)
{
  G4double E_raw = fEdep;  // энергия в МэВ (уже собрана через fScoringVolume)
  
  if (E_raw > 0.) {
    // === Размытие разрешения детектора (NaI) ===
    G4double E_keV = E_raw * 1000.;
    G4double FWHM = 7.5 + 2.5 * std::sqrt(E_keV);  // кэВ
    G4double sigma = (FWHM / 2.355) / 1000.;        // МэВ
    
    G4double E_smeared = G4RandGauss::shoot(E_raw, sigma);
    if (E_smeared < 0.) E_smeared = 0.;
    
    // === Запись в гистограмму ===
    auto analysis = G4AnalysisManager::Instance();
    analysis->FillH1(0, E_smeared);
  }
  
  // === Передаём "сырую" энергию в RunAction для статистики ===
  fRunAction->AddEdep(fEdep);
}

//....oooOO0OOooo........oooOO0OOooo........oooOO0OOooo........oooOO0OOooo......
}  // namespace B1
