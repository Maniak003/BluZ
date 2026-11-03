package ru.starline.bluz

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Адаптер для [androidx.viewpager2.widget.ViewPager2] в [MainActivity].
 *
 * Создаёт 4 фрагмента:
 *  - 0: [SpectrumFragment] — спектр + история (через внутренний swipe-pager)
 *  - 1: [DoseFragment] — дозиметр + статус-pill
 *  - 2: [BluZMapFragment] — карта + GPS-трекинг
 *  - 3: [SettingsFragment] — настройки прибора и приложения
 *
 * Вкладка «Выход» в bottom-nav — это **не страница**, а action. Обрабатывается в
 * [MainActivity.setupNavigation]: при клике вызывается `performExit()`, а не
 * `setCurrentItem`.
 *
 * `HistoryFragment.kt` оставлен в репозитории, но не используется (содержимое перенесено
 * внутрь Spectrum tab после UX-итерации 2026-05-26).
 */
class NumberAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SpectrumFragment()         // Спектр
        1 -> SpectrogrammaFragment()    // Спектрограмма
        2 -> HistoryFragment()          // История
        3 -> DoseFragment()             // Дозиметр
        4 -> BluZMapFragment()          // Карта
        5 -> SettingsFragment()         // Настройки
        else -> SpectrumFragment()
    }
}
