package com.github.jorgecastillo.kotlinandroid.presentation

import arrow.HK
import arrow.syntax.monad.flatMap
import arrow.typeclasses.binding
import com.github.jorgecastillo.kotlinandroid.di.context.SuperHeroesContext
import com.github.jorgecastillo.kotlinandroid.di.context.SuperHeroesContext.GetHeroDetailsContext
import com.github.jorgecastillo.kotlinandroid.di.context.SuperHeroesContext.GetHeroesContext
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError.AuthenticationError
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError.NotFoundError
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError.UnknownServerError
import com.github.jorgecastillo.kotlinandroid.domain.usecase.getHeroDetailsUseCase
import com.github.jorgecastillo.kotlinandroid.domain.usecase.getHeroesUseCase
import com.github.jorgecastillo.kotlinandroid.functional.MonadControl
import com.github.jorgecastillo.kotlinandroid.functional.monadControl
import com.github.jorgecastillo.kotlinandroid.view.viewmodel.SuperHeroViewModel
import com.karumi.marvelapiclient.model.CharacterDto
import com.karumi.marvelapiclient.model.MarvelImage

interface SuperHeroesView {
  fun showNotFoundError()
  fun showGenericError()
  fun showAuthenticationError()
}

interface SuperHeroesListView : SuperHeroesView {
  fun drawHeroes(heroes: List<SuperHeroViewModel>)
}

interface SuperHeroDetailView : SuperHeroesView {
  fun drawHero(hero: SuperHeroViewModel)
}

inline fun <reified F> onHeroListItemClick(heroId: String, C: MonadControl<F, GetHeroesContext, CharacterError>) =
    C.ask().flatMap(C, {
      it.heroDetailsPage.go(heroId, C)
    })

fun displayErrors(ctx: SuperHeroesContext, c: CharacterError): Unit {
  when (c) {
    is NotFoundError -> ctx.view.showNotFoundError()
    is UnknownServerError -> ctx.view.showGenericError()
    is AuthenticationError -> ctx.view.showAuthenticationError()
  }
}

inline fun <reified F> getSuperHeroes(
    C: MonadControl<F, GetHeroesContext, CharacterError> = monadControl()): HK<F, Unit> =
    C.binding {
      val ctx = C.ask().bind()
      val result = C.handleError(getHeroesUseCase(), { displayErrors(ctx, it); emptyList() }).bind()
      ctx.view.drawHeroes(result.map {
        SuperHeroViewModel(
            it.id,
            it.name,
            it.thumbnail.getImageUrl(MarvelImage.Size.PORTRAIT_UNCANNY),
            it.description)
      })
      C.pure(Unit)
    }

inline fun <reified F> getSuperHeroDetails(heroId: String,
    C: MonadControl<F, GetHeroDetailsContext, CharacterError> = monadControl()): HK<F, Unit> =
    C.binding {
      val ctx = C.ask().bind()
      val result = C.handleError(getHeroDetailsUseCase(heroId), { displayErrors(ctx, it); CharacterDto() }).bind()
      ctx.view.drawHero(SuperHeroViewModel(
          result.id,
          result.name,
          result.thumbnail.getImageUrl(MarvelImage.Size.PORTRAIT_UNCANNY),
          result.description))
      C.pure(Unit)
    }
