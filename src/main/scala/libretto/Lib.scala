package libretto

class Lib[DSL <: libretto.DSL](val dsl: DSL) { lib =>
  import dsl._

  def const_[A](a: A): One -⚬ Val[A] =
    andThen(done, const(a))

  /** Evidence that `A` flowing in one direction is equivalent to to `B` flowing in the opposite direction.
    * It must hold that
    * {{{
    *         ┏━━━━━┓                         ┏━━━━━┓
    *         ┞─┐ r ┃                         ┃  l  ┞─┐
    *         ╎A│ I ┃                         ┃  I  ╎B│
    *         ┟─┘ n ┃                         ┃  n  ┟─┘
    *   ┏━━━━━┫   v ┃     ┏━━━━━━━━━┓         ┃  v  ┣━━━━━┓     ┏━━━━━━━━━┓
    *   ┃  l  ┞─┐ e ┃     ┞─┐       ┞─┐       ┃  e  ┞─┐ r ┃     ┞─┐       ┞─┐
    *   ┃  I  ╎B│ r ┃  =  ╎A│ id[A] ╎A│       ┃  r  ╎A│ I ┃  =  ╎B│ id[B] ╎B│
    *   ┃  n  ┟─┘ t ┃     ┟─┘       ┟─┘       ┃  t  ┟─┘ n ┃     ┟─┘       ┟─┘
    *   ┃  v  ┣━━━━━┛     ┗━━━━━━━━━┛         ┗━━━━━┫   v ┃     ┗━━━━━━━━━┛
    *   ┃  e  ┞─┐                                   ┞─┐ e ┃
    *   ┃  r  ╎A│                                   ╎B│ r ┃
    *   ┃  t  ┟─┘                                   ┟─┘ t ┃
    *   ┗━━━━━┛                                     ┗━━━━━┛
    * }}}
    */
  trait Dual[A, B] {
    /** Reverses the input that flows along the `-⚬` arrow (say it is the `A` input) to its dual (`B`) flowing
      * flowing against the direction of the arrow.
      *
      * {{{
      *   ┏━━━━━━━┓
      *   ┞─┐   r ┃
      *   ╎A│─┐ I ┃
      *   ┟─┘ ┆ n ┃
      *   ┃   ┆ v ┃
      *   ┞─┐ ┆ e ┃
      *   ╎B│←┘ r ┃
      *   ┟─┘   t ┃
      *   ┗━━━━━━━┛
      * }}}
      */
    val rInvert: (A |*| B) -⚬ One

    /** Reverses the output that flows against the `-⚬` arrow (say it is the `B` output) to its dual (`A`) flowing
      * in the direction of the arrow.
      *
      * {{{
      *   ┏━━━━━┓
      *   ┃ l   ┞─┐
      *   ┃ I ┌─╎B│
      *   ┃ n ┆ ┟─┘
      *   ┃ v ┆ ┃
      *   ┃ e ┆ ┞─┐
      *   ┃ r └→╎A│
      *   ┃ t   ┟─┘
      *   ┗━━━━━┛
      * }}}
      */
    val lInvert: One -⚬ (B |*| A)

    /** Law stating that [[rInvert]] followed by [[lInvert]] is identity. */
    def law_rl_id: Equal[A -⚬ A] =
      Equal(
        id[A]                   .to[ A               ]
          .introSnd(lInvert)    .to[ A |*| (B |*| A) ]
          .timesAssocRL         .to[ (A |*| B) |*| A ]
          .elimFst(rInvert)     .to[               A ],
        id[A]
      )

    /** Law stating that [[lInvert]] followed by [[rInvert]] is identity. */
    def law_lr_id: Equal[B -⚬ B] =
      Equal(
        id[B]                   .to[               B ]
          .introFst(lInvert)    .to[ (B |*| A) |*| B ]
          .timesAssocLR         .to[ B |*| (A |*| B) ]
          .elimSnd(rInvert)     .to[ B               ],
        id[B]
      )
  }

  object Dual {
    /** Convenience method to summon implicit instances of [[dsl.Dual]]. */
    def apply[A, B](implicit ev: Dual[A, B]): Dual[A, B] = ev
  }

  /** Witnesses that `F` is a covariant endofunctor on the category `-⚬`. */
  trait Functor[F[_]] { self =>
    def lift[A, B](f: A -⚬ B): F[A] -⚬ F[B]

    /** Composition with another covariant functor. */
    def ⚬[G[_]](that: Functor[G]): Functor[λ[x => F[G[x]]]] = new Functor[λ[x => F[G[x]]]] {
      def lift[A, B](f: A -⚬ B): F[G[A]] -⚬ F[G[B]] = self.lift(that.lift(f))
    }

    /** Composition with a contravariant functor. Results in a contravariant functor. */
    def ⚬[G[_]](that: ContraFunctor[G]): ContraFunctor[λ[x => F[G[x]]]] = new ContraFunctor[λ[x => F[G[x]]]] {
      def lift[A, B](f: A -⚬ B): F[G[B]] -⚬ F[G[A]] = self.lift(that.lift(f))
    }
  }

  /** Witnesses that `F` is a contravariant endofunctor on the category `-⚬`. */
  trait ContraFunctor[F[_]] { self =>
    def lift[A, B](f: A -⚬ B): F[B] -⚬ F[A]

    /** Composition with a covariant functor. Results in a contravariant functor. */
    def ⚬[G[_]](that: Functor[G]): ContraFunctor[λ[x => F[G[x]]]] = new ContraFunctor[λ[x => F[G[x]]]] {
      def lift[A, B](f: A -⚬ B): F[G[B]] -⚬ F[G[A]] = self.lift(that.lift(f))
    }

    /** Composition with another contravariant functor. Results in a covariant functor. */
    def ⚬[G[_]](that: ContraFunctor[G]): Functor[λ[x => F[G[x]]]] = new Functor[λ[x => F[G[x]]]] {
      def lift[A, B](f: A -⚬ B): F[G[A]] -⚬ F[G[B]] = self.lift(that.lift(f))
    }
  }

  /** Witnesses that `F` is a bifunctor (covariant in both variables). */
  trait Bifunctor[F[_, _]] {
    def lift[A, B, C, D](f: A -⚬ B, g: C -⚬ D): F[A, C] -⚬ F[B, D]

    def fst[B]: Functor[F[*, B]] = new Functor[F[*, B]] {
      def lift[A1, A2](f: A1 -⚬ A2): F[A1, B] -⚬ F[A2, B] =
        Bifunctor.this.lift[A1, A2, B, B](f, id[B])
    }

    def snd[A]: Functor[F[A, *]] = new Functor[F[A, *]] {
      def lift[B1, B2](g: B1 -⚬ B2): F[A, B1] -⚬ F[A, B2] =
        Bifunctor.this.lift[A, A, B1, B2](id[A], g)
    }

    def inside[G[_]](implicit G: Functor[G]): Bifunctor[λ[(x, y) => G[F[x, y]]]] =
      new Bifunctor[λ[(x, y) => G[F[x, y]]]] {
        def lift[A, B, C, D](f: A -⚬ B, g: C -⚬ D): G[F[A, C]] -⚬ G[F[B, D]] =
          G.lift(Bifunctor.this.lift(f, g))
      }
  }

  object Bifunctor {
    def apply[F[_, _]](implicit ev: Bifunctor[F]): Bifunctor[F] = ev
  }

  /** Represents ''a'' way how `A` can await (join) completion of a concurrent computation. */
  trait Junction[A] {
    def joinL: Done |*| A -⚬ A

    def joinR: A |*| Done -⚬ A =
      swap >>> joinL
  }

  object Junction {
    def junctionDone: Junction[Done] =
      new Junction[Done] {
        override def joinL: Done |*| Done -⚬ Done =
          join
      }

    def junctionVal[A]: Junction[Val[A]] =
      new Junction[Val[A]] {
        override def joinL: Done |*| Val[A] -⚬ Val[A] =
          par(const(()), id[Val[A]]) >>> unliftPair >>> liftV(_._2)
      }
  }

  trait Getter[S, A] { self =>
    def getL[B](that: Getter[A, B])(implicit B: Cosemigroup[B]): S -⚬ (B |*| S)

    def extendJunction(A: Junction[A]): Junction[S]

    def getL(implicit A: Cosemigroup[A]): S -⚬ (A |*| S) =
      getL(Getter.identity[A])

    def getR(implicit A: Cosemigroup[A]): S -⚬ (S |*| A) =
      getL >>> swap

    def joinL(A: Junction[A]): (Done |*| S) -⚬ S =
      extendJunction(A).joinL

    def joinR(A: Junction[A]): (S |*| Done) -⚬ S =
      swap >>> joinL(A)

    def andThen[B](that: Getter[A, B]): Getter[S, B] =
      new Getter[S, B] {
        override def getL[C](next: Getter[B, C])(implicit C: Cosemigroup[C]): S -⚬ (C |*| S) =
          self.getL(that andThen next)

        override def extendJunction(B: Junction[B]): Junction[S] =
          self.extendJunction(that.extendJunction(B))
      }

    def compose[T](that: Getter[T, S]): Getter[T, A] =
      that andThen this

    def |+|[T](that: Getter[T, A]): Getter[S |+| T, A] =
      new Getter[S |+| T, A] {
        override def getL[B](next: Getter[A, B])(implicit B: Cosemigroup[B]): (S |+| T) -⚬ (B |*| (S |+| T)) =
          id[S |+| T].bimap(self.getL(next), that.getL(next)) >>> factorL

        override def extendJunction(A: Junction[A]): Junction[S |+| T] =
          new Junction[S |+| T] {
            override def joinL: Done |*| (S |+| T) -⚬ (S |+| T) =
              distributeLR.bimap(self.joinL(A), that.joinL(A))
          }
      }

    def awaitL[A0](implicit ev: A =:= Val[A0]): (Val[Unit] |*| S) -⚬ S =
      par(discard[Unit] >>> done, id[S]) >>> awaitDoneL[A0]

    def awaitR[A0](implicit ev: A =:= Val[A0]): (S |*| Val[Unit]) -⚬ S =
      swap >>> awaitL

    def awaitDoneL[A0](implicit ev: A =:= Val[A0]): (Done |*| S) -⚬ S =
      ev.substituteCo(this)
        .joinL(Junction.junctionVal[A0])

    def awaitDoneR[A0](implicit ev: A =:= Val[A0]): (S |*| Done) -⚬ S =
      swap >>> awaitDoneL
  }

  object Getter {
    def identity[A]: Getter[A, A] =
      new Getter[A, A] {
        override def getL[B](that: Getter[A, B])(implicit B: Cosemigroup[B]): A -⚬ (B |*| A) =
          that.getL

        override def getL(implicit A: Cosemigroup[A]): A -⚬ (A |*| A) =
          A.split

        override def extendJunction(A: Junction[A]): Junction[A] =
          A
      }
  }

  trait Lens[S, A] extends Getter[S, A] {
    def modify[X, Y](f: (X |*| A) -⚬ (Y |*| A)): (X |*| S) -⚬ (Y |*| S)

    def read[Y](f: A -⚬ (Y |*| A)): S -⚬ (Y |*| S) =
      introFst[S] >>> modify[One, Y](elimFst >>> f)

    def write[X](f: (X |*| A) -⚬ A): (X |*| S) -⚬ S =
      modify[X, One](f >>> introFst) >>> elimFst

    override def getL[B](that: Getter[A, B])(implicit B: Cosemigroup[B]): S -⚬ (B |*| S) =
      read(that.getL)

    override def extendJunction(A: Junction[A]): Junction[S] =
      new Junction[S] {
        def joinL: Done |*| S -⚬ S = write(A.joinL)
      }

    def andThen[B](that: Lens[A, B]): Lens[S, B] =
      new Lens[S, B] {
        def modify[X, Y](f: (X |*| B) -⚬ (Y |*| B)): (X |*| S) -⚬ (Y |*| S) =
          Lens.this.modify(that.modify(f))
      }

    def compose[T](that: Lens[T, S]): Lens[T, A] =
      that andThen this

    def |+|[T](that: Lens[T, A]): Lens[S |+| T, A] =
      new Lens[S |+| T, A] {
        def modify[X, Y](f: (X |*| A) -⚬ (Y |*| A)): (X |*| (S |+| T)) -⚬ (Y |*| (S |+| T)) =
          distributeLR[X, S, T].bimap(Lens.this.modify(f), that.modify(f)) >>> factorL
      }
  }

  object Lens {
    def rec[F[_]]: Lens[Rec[F], F[Rec[F]]] =
      new Lens[Rec[F], F[Rec[F]]] {
        def modify[X, Y](f: (X |*| F[Rec[F]]) -⚬ (Y |*| F[Rec[F]])): (X |*| Rec[F]) -⚬ (Y |*| Rec[F]) =
          id[X |*| Rec[F]]
            .in.snd(unpack)
            .andThen(f)
            .in.snd(pack)
      }
  }

  trait Transportive[F[_]] extends Functor[F] {
    def inL[A, B]: (A |*| F[B]) -⚬ F[A |*| B]
    def outL[A, B]: F[A |*| B] -⚬ (A |*| F[B])

    def inR[A, B]: (F[A] |*| B) -⚬ F[A |*| B] =
      swap[F[A], B] >>> inL >>> lift(swap[B, A])

    def outR[A, B]: F[A |*| B] -⚬ (F[A] |*| B) =
      lift(swap[A, B]) >>> outL >>> swap[B, F[A]]

    def getL[A](implicit A: Cosemigroup[A]): F[A] -⚬ (A |*| F[A]) =
      lift(A.split) >>> outL

    def getR[A](implicit A: Cosemigroup[A]): F[A] -⚬ (F[A] |*| A) =
      getL[A] >>> swap

    def lens[A]: Lens[F[A], A] = new Lens[F[A], A] {
      def modify[X, Y](f: (X |*| A) -⚬ (Y |*| A)): (X |*| F[A]) -⚬ (Y |*| F[A]) =
        inL >>> lift(f) >>> outL
    }
  }

  trait Unapply[FA, F[_]] {
    type A
    def ev: FA =:= F[A]
  }

  object Unapply {
    implicit def unapplyInstance[F[_], X]: Unapply[F[X], F] { type A = X } =
      new Unapply[F[X], F] {
        type A = X
        val ev: F[X] =:= F[A] = implicitly
      }
  }

  trait Unapply2[FAB, F[_, _]] {
    type A
    type B
    def ev: FAB =:= F[A, B]
  }

  object Unapply2 {
    implicit def unapply2Instance[F[_, _], X, Y]: Unapply2[F[X, Y], F] { type A = X; type B = Y } =
      new Unapply2[F[X, Y], F] {
        type A = X
        type B = Y
        val ev: F[X, Y] =:= F[A, B] = implicitly
      }
  }

  def liftFst[A, B, C](f: A -⚬ C): (A |*| B) -⚬ (C |*| B) = par(f, id)
  def liftSnd[A, B, C](f: B -⚬ C): (A |*| B) -⚬ (A |*| C) = par(id, f)

  def liftL[A, B, C](f: A -⚬ C): (A |+| B) -⚬ (C |+| B) = either(f andThen injectL, injectR)
  def liftR[A, B, C](f: B -⚬ C): (A |+| B) -⚬ (A |+| C) = either(injectL, f andThen injectR)

  type Id[A] = A

  implicit val idFunctor: Transportive[Id] = new Transportive[Id] {
    def lift[A, B](f: A -⚬ B): Id[A] -⚬ Id[B] = f
    def inL[A, B]: (A |*| Id[B]) -⚬ Id[A |*| B] = id
    def outL[A, B]: Id[A |*| B] -⚬ (A |*| Id[B]) = id
  }

  /** Product is covariant in the first argument. */
  implicit def fst[B]: Transportive[* |*| B] = new Transportive[* |*| B] {
    def lift[A1, A2](f: A1 -⚬ A2): (A1 |*| B) -⚬ (A2 |*| B) = liftFst(f)
    def inL[A1, A2]: (A1 |*| (A2 |*| B)) -⚬ ((A1 |*| A2) |*| B) = timesAssocRL
    def outL[A1, A2]: ((A1 |*| A2) |*| B) -⚬ (A1 |*| (A2 |*| B)) = timesAssocLR
  }

  /** Product is covariant in the second argument. */
  implicit def snd[A]: Transportive[A |*| *] = new Transportive[A |*| *] {
    def lift[B1, B2](f: B1 -⚬ B2): (A |*| B1) -⚬ (A |*| B2) = liftSnd(f)
    def inL[B1, B2]: (B1 |*| (A |*| B2)) -⚬ (A |*| (B1 |*| B2)) =
      timesAssocRL[B1, A, B2].in.fst(swap).timesAssocLR
    def outL[B1, B2]: (A |*| (B1 |*| B2)) -⚬ (B1 |*| (A |*| B2)) =
      timesAssocRL[A, B1, B2].in.fst(swap).timesAssocLR
  }

  /** Disjoint union is covariant in the left argument. */
  def left[B]: Functor[* |+| B] = new Functor[* |+| B] {
    def lift[A1, A2](f: A1 -⚬ A2): (A1 |+| B) -⚬ (A2 |+| B) = liftL(f)
  }

  /** Disjoint union is covariant in the right argument. */
  def right[A]: Functor[A |+| *] = new Functor[A |+| *] {
    def lift[B1, B2](f: B1 -⚬ B2): (A |+| B1) -⚬ (A |+| B2) = liftR(f)
  }

  /** Choice is covariant in the left argument. */
  def choiceL[B]: Functor[* |&| B] = new Functor[* |&| B] {
    def lift[A1, A2](f: A1 -⚬ A2): (A1 |&| B) -⚬ (A2 |&| B) = choice[A1 |&| B, A2, B](chooseL andThen f, chooseR)
  }

  /** Choice is covariant in the right argument. */
  def choiceR[A]: Functor[A |&| *] = new Functor[A |&| *] {
    def lift[B1, B2](f: B1 -⚬ B2): (A |&| B1) -⚬ (A |&| B2) = choice[A |&| B1, A, B2](chooseL, chooseR andThen f)
  }

  /** Function object (exponential) is contravariant in the input type. */
  def input[C]: ContraFunctor[* =⚬ C] = new ContraFunctor[* =⚬ C] {
    def lift[A, B](f: A -⚬ B): (B =⚬ C) -⚬ (A =⚬ C) =
      id                       [(B =⚬ C) |*| A]
        .in.snd(f)          .to[(B =⚬ C) |*| B]
        .andThen(eval)      .to[C]
        .as[((B =⚬ C) |*| A) -⚬ C]
        .curry
  }

  /** Function object (exponential) is covariant in the output type. */
  def output[A]: Functor[A =⚬ *] = new Functor[A =⚬ *] {
    def lift[B, C](f: B -⚬ C): (A =⚬ B) -⚬ (A =⚬ C) =
      id                       [(A =⚬ B) |*| A]
        .andThen(eval)      .to[B]
        .andThen(f)         .to[C]
        .as[((A =⚬ B) |*| A) -⚬ C]
        .curry
  }

  implicit val tensorBifunctor: Bifunctor[|*|]= new Bifunctor[|*|] {
    def lift[A, B, C, D](f: A -⚬ B, g: C -⚬ D): (A |*| C) -⚬ (B |*| D) =
      par(f, g)
  }

  implicit val eitherBifunctor: Bifunctor[|+|] = new Bifunctor[|+|] {
    def lift[A, B, C, D](f: A -⚬ B, g: C -⚬ D): (A |+| C )-⚬ (B |+| D) =
      either(f andThen injectL, g andThen injectR)
  }

  implicit val choiceBifunctor: Bifunctor[|&|]= new Bifunctor[|&|] {
    def lift[A, B, C, D](f: A -⚬ B, g: C -⚬ D): (A |&| C) -⚬ (B |&| D) =
      choice(chooseL andThen f, chooseR andThen g)
  }

  implicit class LinearFunctionOps[A, B](self: A -⚬ B) {
    /** No-op used for documentation purposes: explicitly states the input type of this linear function. */
    def from[Z](implicit ev: A =:= Z): Z -⚬ B = ev.substituteCo[* -⚬ B](self)

    /** No-op used for documentation purposes: explicitly states the output type of this linear function. */
    def to[C](implicit ev: B =:= C): A -⚬ C = ev.substituteCo(self)

    /** No-op used for documentation purposes: explicitly states the full type of this linear function. */
    def as[C](implicit ev: (A -⚬ B) =:= C): C = ev(self)

    def andThen[C](g: B -⚬ C): A -⚬ C = dsl.andThen(self, g)

    def bimap[F[_, _]]: BimapSyntax[F, A, B] =
      new BimapSyntax[F, A, B](self)

    /** Alias for [[andThen]]. */
    def >>>[C](g: B -⚬ C): A -⚬ C = this andThen g

    def injectL[C]: A -⚬ (B |+| C) =
      dsl.andThen(self, dsl.injectL)

    def injectR[C]: A -⚬ (C |+| B) =
      dsl.andThen(self, dsl.injectR)

    def either[B1, B2, C](f: B1 -⚬ C, g: B2 -⚬ C)(implicit ev: B =:= (B1 |+| B2)): A -⚬ C =
      dsl.andThen(ev.substituteCo(self), dsl.either(f, g))

    def chooseL[B1, B2](implicit ev: B =:= (B1 |&| B2)): A -⚬ B1 =
      ev.substituteCo(self) >>> dsl.chooseL

    def chooseR[B1, B2](implicit ev: B =:= (B1 |&| B2)): A -⚬ B2 =
      ev.substituteCo(self) >>> dsl.chooseR

    def choice[C, D](f: B -⚬ C, g: B -⚬ D): A -⚬ (C |&| D) =
      self >>> dsl.choice(f, g)

    def par[B1, B2, C, D](f: B1 -⚬ C, g: B2 -⚬ D)(implicit ev: B =:= (B1 |*| B2)): A -⚬ (C |*| D) =
      ev.substituteCo(self) >>> dsl.par(f, g)

    def elimFst[B2](implicit ev: B =:= (One |*| B2)): A -⚬ B2 =
      ev.substituteCo(self) >>> dsl.elimFst

    def elimFst[B1, B2](f: B1 -⚬ One)(implicit ev: B =:= (B1 |*| B2)): A -⚬ B2 =
      ev.substituteCo(self) >>> dsl.elimFst(f)

    def elimSnd[B1](implicit ev: B =:= (B1 |*| One)): A -⚬ B1 =
      ev.substituteCo(self) >>> dsl.elimSnd

    def elimSnd[B1, B2](f: B2 -⚬ One)(implicit ev: B =:= (B1 |*| B2)): A -⚬ B1 =
      ev.substituteCo(self) >>> dsl.elimSnd(f)

    def introFst: A -⚬ (One |*| B) =
      self >>> dsl.introFst

    def introFst[C](f: One -⚬ C): A -⚬ (C |*| B) =
      self >>> dsl.introFst(f)

    def introSnd: A -⚬ (B |*| One) =
      self >>> dsl.introSnd

    def introSnd[C](f: One -⚬ C): A -⚬ (B |*| C) =
      self >>> dsl.introSnd(f)

    def swap[B1, B2](implicit ev: B =:= (B1 |*| B2)): A -⚬ (B2 |*| B1) =
      ev.substituteCo(self) >>> dsl.swap

    def curry[A1, A2](implicit ev: A =:= (A1 |*| A2)): A1 -⚬ (A2 =⚬ B) =
      dsl.curry(ev.substituteCo[* -⚬ B](self))

    def uncurry[B1, B2](implicit ev: B =:= (B1 =⚬ B2)): (A |*| B1) -⚬ B2 =
      dsl.uncurry(ev.substituteCo(self))

    def timesAssocLR[B1, B2, B3](implicit ev: B =:= ((B1 |*| B2) |*| B3)): A -⚬ (B1 |*| (B2 |*| B3)) =
      ev.substituteCo(self) >>> dsl.timesAssocLR

    def timesAssocRL[B1, B2, B3](implicit ev: B =:= (B1 |*| (B2 |*| B3))): A -⚬ ((B1 |*| B2) |*| B3) =
      ev.substituteCo(self) >>> dsl.timesAssocRL

    def plusAssocLR[B1, B2, B3](implicit ev: B =:= ((B1 |+| B2) |+| B3)): A -⚬ (B1 |+| (B2 |+| B3)) =
      ev.substituteCo(self) >>> dsl.plusAssocLR

    def plusAssocRL[B1, B2, B3](implicit ev: B =:= (B1 |+| (B2 |+| B3))): A -⚬ ((B1 |+| B2) |+| B3) =
      ev.substituteCo(self) >>> dsl.plusAssocRL

    def distributeLR[B1, B2, B3](implicit ev: B =:= (B1 |*| (B2 |+| B3))): A -⚬ ((B1 |*| B2) |+| (B1 |*| B3)) =
      ev.substituteCo(self) >>> dsl.distributeLR

    def distributeRL[B1, B2, B3](implicit ev: B =:= ((B1 |+| B2) |*| B3)): A -⚬ ((B1 |*| B3) |+| (B2 |*| B3)) =
      ev.substituteCo(self) >>> dsl.distributeRL

    def coFactorL[B1, B2, B3](implicit ev: B =:= (B1 |*| (B2 |&| B3))): A -⚬ ((B1 |*| B2) |&| (B1 |*| B3)) =
      ev.substituteCo(self) >>> dsl.coFactorL

    def coFactorR[B1, B2, B3](implicit ev: B =:= ((B1 |&| B2) |*| B3)): A -⚬ ((B1 |*| B3) |&| (B2 |*| B3)) =
      ev.substituteCo(self) >>> dsl.coFactorR

    def pack[F[_]](implicit ev: B =:= F[Rec[F]]): A -⚬ Rec[F] =
      ev.substituteCo(self) >>> dsl.pack[F]

    def unpack[F[_]](implicit ev: B =:= Rec[F]): A -⚬ F[Rec[F]] =
      ev.substituteCo(self) >>> dsl.unpack[F]

    def race[B1: Completive, B2: Completive, C](
      caseFstWins: (B1 |*| B2) -⚬ C,
      caseSndWins: (B1 |*| B2) -⚬ C,
    )(implicit
      ev: B =:= (B1 |*| B2),
    ): A -⚬ C =
      ev.substituteCo(self) >>> dsl.race(caseFstWins, caseSndWins)

    def select[C1: Requisitive, C2: Requisitive](
      caseFstWins: B -⚬ (C1 |*| C2),
      caseSndWins: B -⚬ (C1 |*| C2),
    ): A -⚬ (C1 |*| C2) =
      self >>> dsl.select(caseFstWins, caseSndWins)

    def in: FocusedFunctionOutputCo[A, Id, B] = new FocusedFunctionOutputCo[A, Id, B](self)(idFunctor)
  }

  class BimapSyntax[F[_, _], A, B](self: A -⚬ B) {
    def apply[B1, B2, C, D](
      f: B1 -⚬ C,
      g: B2 -⚬ D,
    )(implicit
      ev: B =:= F[B1, B2],
      F: Bifunctor[F],
    ): A -⚬ F[C, D] =
      dsl.andThen(ev.substituteCo(self), F.lift(f, g))
  }

  /** Focused on `B` in the output `F[B]` of linear function `A -⚬ F[B]`, where `B` is in a covariant position. */
  class FocusedFunctionOutputCo[A, F[_], B](f: A -⚬ F[B])(F: Functor[F]) {
    def map[C](g: B -⚬ C): A -⚬ F[C] = f andThen F.lift(g)

    /** Alias for [[map]]. */
    def apply[C](g: B -⚬ C): A -⚬ F[C] = map(g)

    def subst[C](implicit ev: B =:= C): A -⚬ F[C] =
      ev.liftCo[F].substituteCo(f)

    def unsubst[C](implicit ev: C =:= B): A -⚬ F[C] =
      ev.liftCo[F].substituteContra(f)

    def zoomCo[G[_], C](G: Functor[G])(implicit ev: B =:= G[C]): FocusedFunctionOutputCo[A, λ[x => F[G[x]]], C] =
      new FocusedFunctionOutputCo[A, λ[x => F[G[x]]], C](ev.liftCo[F].substituteCo(f))(F ⚬ G)

    def zoomContra[G[_], C](G: ContraFunctor[G])(implicit ev: B =:= G[C]): FocusedFunctionOutputContra[A, λ[x => F[G[x]]], C] =
      new FocusedFunctionOutputContra[A, λ[x => F[G[x]]], C](ev.liftCo[F].substituteCo(f))(F ⚬ G)

    def co[G[_]](implicit G: Functor[G], U: Unapply[B, G]): FocusedFunctionOutputCo[A, λ[x => F[G[x]]], U.A] =
      zoomCo[G, U.A](G)(U.ev)

    def contra[G[_]](implicit G: ContraFunctor[G], U: Unapply[B, G]): FocusedFunctionOutputContra[A, λ[x => F[G[x]]], U.A] =
      zoomContra[G, U.A](G)(U.ev)

    def bi[G[_, _]](implicit G: Bifunctor[G], U: Unapply2[B, G]): FocusedFunctionOutputBi[A, λ[(x, y) => F[G[x, y]]], U.A, U.B] =
      new FocusedFunctionOutputBi[A, λ[(x, y) => F[G[x, y]]], U.A, U.B](U.ev.liftCo[F].substituteCo(f))(G inside F)

    def injectL[C]: A -⚬ F[B |+| C] = f andThen F.lift(dsl.injectL)
    def injectR[C]: A -⚬ F[C |+| B] = f andThen F.lift(dsl.injectR)
  }

  class FocusedFunctionOutputBi[A, F[_, _], B1, B2](f: A -⚬ F[B1, B2])(F: Bifunctor[F]) {
    def fst: FocusedFunctionOutputCo[A, F[*, B2], B1] =
      new FocusedFunctionOutputCo[A, F[*, B2], B1](f)(F.fst)

    def snd: FocusedFunctionOutputCo[A, F[B1, *], B2] =
      new FocusedFunctionOutputCo[A, F[B1, *], B2](f)(F.snd)
  }

  implicit class FocusedFunctionOutputOnTimesCo[A, F[_], B1, B2](f: FocusedFunctionOutputCo[A, F, B1 |*| B2]) {
    def fst: FocusedFunctionOutputCo[A, λ[x => F[x |*| B2]], B1] =
      f.zoomCo(lib.fst[B2])

    def snd: FocusedFunctionOutputCo[A, λ[x => F[B1 |*| x]], B2] =
      f.zoomCo(lib.snd[B1])
  }

  implicit class FocusedFunctionOutputOnPlusCo[A, F[_], B1, B2](f: FocusedFunctionOutputCo[A, F, B1 |+| B2]) {
    def left: FocusedFunctionOutputCo[A, λ[x => F[x |+| B2]], B1] =
      f.zoomCo(lib.left[B2])

    def right: FocusedFunctionOutputCo[A, λ[x => F[B1 |+| x]], B2] =
      f.zoomCo(lib.right[B1])
  }

  implicit class FocusedFunctionOutputOnChoiceCo[A, F[_], B1, B2](f: FocusedFunctionOutputCo[A, F, B1 |&| B2]) {
    def choiceL: FocusedFunctionOutputCo[A, λ[x => F[x |&| B2]], B1] =
      f.zoomCo(lib.choiceL[B2])

    def choiceR: FocusedFunctionOutputCo[A, λ[x => F[B1 |&| x]], B2] =
      f.zoomCo(lib.choiceR[B1])
  }

  implicit class FocusedFunctionOutputOnFunctionCo[A, F[_], B1, B2](f: FocusedFunctionOutputCo[A, F, B1 =⚬ B2]) {
    def input: FocusedFunctionOutputContra[A, λ[x => F[x =⚬ B2]], B1] =
      f.zoomContra(lib.input[B2])

    def output: FocusedFunctionOutputCo[A, λ[x => F[B1 =⚬ x]], B2] =
      f.zoomCo(lib.output[B1])
  }

  /** Focused on `B` in the output `F[B]` of linear function `A -⚬ F[B]`, where `B` is in a contravariant position. */
  class FocusedFunctionOutputContra[A, F[_], B](f: A -⚬ F[B])(F: ContraFunctor[F]) {
    def unapply[B0](g: B0 -⚬ B): A -⚬ F[B0] = f andThen F.lift(g)

    def subst[C](implicit ev: B =:= C): A -⚬ F[C] =
      ev.liftCo[F].substituteCo(f)

    def unsubst[C](implicit ev: C =:= B): A -⚬ F[C] =
      ev.liftCo[F].substituteContra(f)

    def zoomCo[G[_], C](G: Functor[G])(implicit ev: B =:= G[C]): FocusedFunctionOutputContra[A, λ[x => F[G[x]]], C] =
      new FocusedFunctionOutputContra[A, λ[x => F[G[x]]], C](ev.liftCo[F].substituteCo(f))(F ⚬ G)

    def zoomContra[G[_], C](G: ContraFunctor[G])(implicit ev: B =:= G[C]): FocusedFunctionOutputCo[A, λ[x => F[G[x]]], C] =
      new FocusedFunctionOutputCo[A, λ[x => F[G[x]]], C](ev.liftCo[F].substituteCo(f))(F ⚬ G)

    def co[G[_]](implicit G: Functor[G], U: Unapply[B, G]): FocusedFunctionOutputContra[A, λ[x => F[G[x]]], U.A] =
      zoomCo[G, U.A](G)(U.ev)

    def contra[G[_]](implicit G: ContraFunctor[G], U: Unapply[B, G]): FocusedFunctionOutputCo[A, λ[x => F[G[x]]], U.A] =
      zoomContra[G, U.A](G)(U.ev)
  }

  implicit class FocusedFunctionOutputOnTimesContra[A, F[_], B1, B2](f: FocusedFunctionOutputContra[A, F, B1 |*| B2]) {
    def fst: FocusedFunctionOutputContra[A, λ[x => F[x |*| B2]], B1] =
      f.zoomCo(lib.fst[B2])

    def snd: FocusedFunctionOutputContra[A, λ[x => F[B1 |*| x]], B2] =
      f.zoomCo(lib.snd[B1])
  }

  implicit class FocusedFunctionOutputOnPlusContra[A, F[_], B1, B2](f: FocusedFunctionOutputContra[A, F, B1 |+| B2]) {
    def left: FocusedFunctionOutputContra[A, λ[x => F[x |+| B2]], B1] =
      f.zoomCo(lib.left[B2])

    def right: FocusedFunctionOutputContra[A, λ[x => F[B1 |+| x]], B2] =
      f.zoomCo(lib.right[B1])
  }

  implicit class FocusedFunctionOutputOnChoiceContra[A, F[_], B1, B2](f: FocusedFunctionOutputContra[A, F, B1 |&| B2]) {
    def choiceL: FocusedFunctionOutputContra[A, λ[x => F[x |&| B2]], B1] =
      f.zoomCo(lib.choiceL[B2])

    def choiceR: FocusedFunctionOutputContra[A, λ[x => F[B1 |&| x]], B2] =
      f.zoomCo(lib.choiceR[B1])
  }

  implicit class FocusedFunctionOutputOnFunctionContra[A, F[_], B1, B2](f: FocusedFunctionOutputContra[A, F, B1 =⚬ B2]) {
    def input: FocusedFunctionOutputCo[A, λ[x => F[x =⚬ B2]], B1] =
      f.zoomContra(lib.input[B2])

    def output: FocusedFunctionOutputContra[A, λ[x => F[B1 =⚬ x]], B2] =
      f.zoomCo(lib.output[B1])
  }

  def IXI[A, B, C, D]: ((A|*|B)|*|(C|*|D)) -⚬
  //                     |    \   /    |
  //                     |     \ /     |
  //                     |      X      |
  //                     |     / \     |
  //                     |    /   \    |
                       ((A|*|C)|*|(B|*|D)) =
    id                             [ (A |*| B) |*| (C |*| D) ]
      .timesAssocLR             .to[ A |*| (B |*| (C |*| D)) ]
      .in.snd(timesAssocRL)     .to[ A |*| ((B |*| C) |*| D) ]
      .in.snd.fst(swap)         .to[ A |*| ((C |*| B) |*| D) ]
      .in.snd(timesAssocLR)     .to[ A |*| (C |*| (B |*| D)) ]
      .timesAssocRL             .to[ (A |*| C) |*| (B |*| D) ]

  def IX[A, B, C]: ((A|*|B)|*| C) -⚬
    //               |    \   /
    //               |     \ /
    //               |      X
    //               |     / \
    //               |    /   \
                   ((A|*|C)|*| B) =
    timesAssocLR[A, B, C] >>> par(id, swap) >>> timesAssocRL

  def XI[A, B, C]: (A |*|(B|*|C)) -⚬
    //               \   /    |
    //                \ /     |
    //                 X      |
    //                / \     |
    //               /   \    |
                   (B |*|(A|*|C)) =
    timesAssocRL[A, B, C] >>> par(swap, id) >>> timesAssocLR

  @deprecated("uses deprecated Comonoid[Val[A]]")
  def fakeDemand[A]: One -⚬ Neg[A] =
    id                                           [        One        ]
      .andThen(promise[A])                    .to[ Neg[A] |*| Val[A] ]
      .andThen(discardSnd)                    .to[ Neg[A]            ]

  def mergeDemands[A]: (Neg[A] |*| Neg[A]) -⚬ Neg[A] =
    id                                         [                                       Neg[A] |*| Neg[A]   ]
      .introFst(promise[A])                 .to[ (Neg[A] |*|        Val[A]      ) |*| (Neg[A] |*| Neg[A])  ]
      .timesAssocLR                         .to[  Neg[A] |*| (      Val[A]        |*| (Neg[A] |*| Neg[A])) ]
      .in.snd.fst(dup)                      .to[  Neg[A] |*| ((Val[A] |*| Val[A]) |*| (Neg[A] |*| Neg[A])) ]
      .in.snd(IXI)                          .to[  Neg[A] |*| ((Val[A] |*| Neg[A]) |*| (Val[A] |*| Neg[A])) ]
      .in.snd(parToOne(fulfill, fulfill))   .to[  Neg[A] |*|                      One                      ]
      .elimSnd                              .to[  Neg[A]                                                   ]

  /** From the choice ''available'' on the right (`C |&| D`), choose the one corresponding to the choice ''made''
    * on the left (`A |+| B`): if on the left there is `A`, choose `C`, if on the left thre is `B`, choose `D`.
    */
  def matchingChoiceLR[A, B, C, D]: ((A |+| B) |*| (C |&| D)) -⚬ ((A |*| C) |+| (B |*| D)) =
    id[(A |+| B) |*| (C |&| D)]
      .distributeRL            .to[(A |*| (C |&| D)) |+| (B |*| (C |&| D))]
      .in.left.snd(chooseL)    .to[(A |*|  C       ) |+| (B |*| (C |&| D))]
      .in.right.snd(chooseR)   .to[(A |*|  C       ) |+| (B |*|        D )]

  /** From the choice ''available'' on the left (`A |&| B`), choose the one corresponding to the choice ''made''
    * on the right (`C |+| D`): if on the right there is `C`, choose `A`, if on the right there is `D`, choose `B`.
    */
  def matchingChoiceRL[A, B, C, D]: ((A |&| B) |*| (C |+| D)) -⚬ ((A |*| C) |+| (B |*| D)) =
    id[(A |&| B) |*| (C |+| D)]
      .distributeLR            .to[((A |&| B) |*| C) |+| ((A |&| B) |*| D)]
      .in.left.fst(chooseL)    .to[( A        |*| C) |+| ((A |&| B) |*| D)]
      .in.right.fst(chooseR)   .to[( A        |*| C) |+| (       B  |*| D)]

  /** Creates a pair of mutually recursive functions. */
  def rec2[A, B, C, D](
    f: (A -⚬ B, C -⚬ D) => A -⚬ B,
    g: (A -⚬ B, C -⚬ D) => C -⚬ D,
  ): (A -⚬ B, C -⚬ D) =
    (
      rec { (ab: A -⚬ B) => f(ab, rec { (cd: C -⚬ D) => g(ab, cd) }) },
      rec { (cd: C -⚬ D) => g(rec { (ab: A -⚬ B) => f(ab, cd) }, cd) },
    )

  type Bool = Val[Unit] |+| Val[Unit]
  object Bool {
    val constTrue: One -⚬ Bool =
      const_(()) >>> injectL

    val constFalse: One -⚬ Bool =
      const_(()) >>> injectR

    def ifThenElse[A, B, C](ifTrue: Val[Unit] |*| A -⚬ B, ifFalse: Val[Unit] |*| A -⚬ C): (Bool |*| A) -⚬ (B |+| C) =
      id                                   [               Bool |*| A                ]
        .distributeRL                   .to[ (Val[Unit] |*| A) |+| (Val[Unit] |*| A) ]
        .bimap(ifTrue, ifFalse)         .to[             B     |+|             C     ]

    private val eitherToBoolean: Either[Unit, Unit] => Boolean = {
      case Left(())  => true
      case Right(()) => false
    }

    private val booleanToEither: Boolean => Either[Unit, Unit] = {
      case true => Left(())
      case false => Right(())
    }

    def liftBoolean: Val[Boolean] -⚬ Bool = {
      id                                     [ Val[Boolean]            ]
        .andThen(liftV(booleanToEither))  .to[ Val[Either[Unit, Unit]] ]
        .andThen(liftEither)              .to[ Val[Unit] |+| Val[Unit] ]
    }

    def unliftBoolean: Bool -⚬ Val[Boolean] =
      id[Bool]                            .to[ Val[Unit] |+| Val[Unit] ]
      .andThen(unliftEither)              .to[ Val[Either[Unit, Unit]] ]
      .andThen(liftV(eitherToBoolean))    .to[      Val[Boolean]       ]
  }

  import Bool._

  def liftBipredicate[A, B](p: (A, B) => Boolean): (Val[A] |*| Val[B]) -⚬ Bool =
    id                                            [ Val[A] |*| Val[B] ]
      .andThen(unliftPair)                     .to[   Val[(A, B)]     ]
      .andThen(liftV(p.tupled))                .to[   Val[Boolean]    ]
      .andThen(liftBoolean)                    .to[       Bool        ]

  def lt[A](implicit ord: Ordering[A]): (Val[A] |*| Val[A]) -⚬ Bool =
    liftBipredicate(ord.lt)

  def lteq[A](implicit ord: Ordering[A]): (Val[A] |*| Val[A]) -⚬ Bool =
    liftBipredicate(ord.lteq)

  def gt[A](implicit ord: Ordering[A]): (Val[A] |*| Val[A]) -⚬ Bool =
    liftBipredicate(ord.gt)

  def gteq[A](implicit ord: Ordering[A]): (Val[A] |*| Val[A]) -⚬ Bool =
    liftBipredicate(ord.gteq)

  def equiv[A](implicit ord: Ordering[A]): (Val[A] |*| Val[A]) -⚬ Bool =
    liftBipredicate(ord.equiv)

  private def testKeys[A, B, K](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
    pred: (K, K) => Boolean,
  ): (A |*| B) -⚬ ((A |*| B) |+| (A |*| B)) = {
    val awaitL: (Val[Unit] |*| (A |*| B)) -⚬ (A |*| B) =
      (aKey compose fst[B].lens[A]).awaitL

    import lib.comonoidVal

    id[A |*| B]
      .par(aKey.getL, bKey.getL)
      .andThen(IXI)
      .in.fst(liftBipredicate(pred))
      .andThen(ifThenElse(awaitL, awaitL))
  }


  def ltBy[A, B, K](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
  )(implicit
    ord: Ordering[K],
  ): (A |*| B) -⚬ ((A |*| B) |+| (A |*| B)) =
    testKeys(aKey, bKey, ord.lt)

  def lteqBy[A, B, K](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
  )(implicit
    ord: Ordering[K],
  ): (A |*| B) -⚬ ((A |*| B) |+| (A |*| B)) =
    testKeys(aKey, bKey, ord.lteq)

  def gtBy[A, B, K](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
  )(implicit
    ord: Ordering[K],
  ): (A |*| B) -⚬ ((A |*| B) |+| (A |*| B)) =
    testKeys(aKey, bKey, ord.gt)

  def gteqBy[A, B, K](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
  )(implicit
    ord: Ordering[K],
  ): (A |*| B) -⚬ ((A |*| B) |+| (A |*| B)) =
    testKeys(aKey, bKey, ord.gteq)

  def equivBy[A, B, K](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
  )(implicit
    ord: Ordering[K],
  ): (A |*| B) -⚬ ((A |*| B) |+| (A |*| B)) =
    testKeys(aKey, bKey, ord.equiv)

  def sortBy[A, B, K: Ordering](
    aKey: Getter[A, Val[K]],
    bKey: Getter[B, Val[K]],
  )
  : (A |*| B) -⚬ ((A |*| B) |+| (B |*| A)) =
    lteqBy(aKey, bKey).in.right(swap)

  sealed trait CompareModule {
    type Compared[A, B]

    def compareBy[A, B, K: Ordering](
      aKey: Getter[A, Val[K]],
      bKey: Getter[B, Val[K]],
    ): A |*| B -⚬ Compared[A, B]

    def compared[A, B, C](
      caseLt: (A |*| B) -⚬ C,
      caseEq: (A |*| B) -⚬ C,
      caseGt: (A |*| B) -⚬ C,
    )
    : Compared[A, B] -⚬ C

    implicit def bifunctorCompared: Bifunctor[Compared]
  }

  val Compare: CompareModule = new CompareModule {
    type Compared[A, B] = (A |*| B) |+| ((A |*| B) |+| (A |*| B))

    def compareBy[A, B, K: Ordering](
      aKey: Getter[A, Val[K]],
      bKey: Getter[B, Val[K]],
    ): A |*| B -⚬ Compared[A, B] =
      id                                           [           A |*| B                       ]
        .andThen(ltBy(aKey, bKey))              .to[ (A |*| B) |+|           (A |*| B)       ]
        .in.right(equivBy(aKey, bKey))          .to[ (A |*| B) |+| ((A |*| B) |+| (A |*| B)) ]

    def compared[A, B, C](
      caseLt: A |*| B -⚬ C,
      caseEq: A |*| B -⚬ C,
      caseGt: A |*| B -⚬ C,
    ): Compared[A, B] -⚬ C =
      either(caseLt, either(caseEq, caseGt))

    override def bifunctorCompared: Bifunctor[Compared] =
      new Bifunctor[Compared] {
        def lift[A, B, C, D](f: A -⚬ B, g: C -⚬ D): Compared[A, C] -⚬ Compared[B, D] = {
          Bifunctor[|+|].lift(
            par(f, g),
            Bifunctor[|+|].lift(
              par(f, g),
              par(f, g),
            )
          )
        }
      }
  }

  def zapPremises[A, Ā, B, C](implicit ev: Dual[A, Ā]): ((A =⚬ B) |*| (Ā =⚬ C)) -⚬ (B |*| C) = {
    id                              [  (A =⚬ B) |*| (Ā =⚬ C)                ]
      .introSnd(ev.lInvert)      .to[ ((A =⚬ B) |*| (Ā =⚬ C)) |*| (Ā |*| A) ]
      .in.snd(swap)              .to[ ((A =⚬ B) |*| (Ā =⚬ C)) |*| (A |*| Ā) ]
      .andThen(IXI)              .to[ ((A =⚬ B) |*| A) |*| ((Ā =⚬ C) |*| Ā) ]
      .andThen(par(eval, eval))  .to[        B         |*|        C         ]
  }

  def dualSymmetric[A, B](ev: Dual[A, B]): Dual[B, A] = new Dual[B, A] {
    val lInvert: One -⚬ (A |*| B) = andThen(ev.lInvert, swap)
    val rInvert: B |*| A -⚬ One = andThen(swap, ev.rInvert)
  }

  implicit def oneSelfDual: Dual[One, One] = new Dual[One, One] {
    val lInvert: One -⚬ (One |*| One) = introSnd
    val rInvert: One |*| One -⚬ One = elimSnd
  }

  def rInvertTimes[A, B, Ȧ, Ḃ](
    rInvertA: A |*| Ȧ -⚬ One,
    rInvertB: B |*| Ḃ -⚬ One,
  ): ((A |*| B) |*| (Ȧ |*| Ḃ)) -⚬ One =
    id[(A |*| B) |*| (Ȧ |*| Ḃ)]               .to[ (A |*| B) |*| (Ȧ |*| Ḃ) ]
      .andThen(IXI)                           .to[ (A |*| Ȧ) |*| (B |*| Ḃ) ]
      .andThen(parToOne(rInvertA, rInvertB))  .to[           One           ]

  def lInvertTimes[A, B, Ȧ, Ḃ](
    lInvertA: One -⚬ (Ȧ |*| A),
    lInvertB: One -⚬ (Ḃ |*| B),
  ): One -⚬ ((Ȧ |*| Ḃ) |*| (A |*| B)) =
    id[One]                                   .to[           One           ]
      .andThen(parFromOne(id, id))            .to[    One    |*|    One    ]
      .par(lInvertA, lInvertB)                .to[ (Ȧ |*| A) |*| (Ḃ |*| B) ]
      .andThen(IXI)                           .to[ (Ȧ |*| Ḃ) |*| (A |*| B) ]

  implicit def productDuality[A, B, Ȧ, Ḃ](implicit a: Dual[A, Ȧ], b: Dual[B, Ḃ]): Dual[A |*| B, Ȧ |*| Ḃ] =
    new Dual[A |*| B, Ȧ |*| Ḃ] {
      val lInvert: One -⚬ ((Ȧ |*| Ḃ) |*| (A |*| B)) =
        lInvertTimes(a.lInvert, b.lInvert)

      val rInvert: ((A |*| B) |*| (Ȧ |*| Ḃ)) -⚬ One =
        rInvertTimes(a.rInvert, b.rInvert)
    }

  def rInvertEither[A, B, Ȧ, Ḃ](
    rInvertA: A |*| Ȧ -⚬ One,
    rInvertB: B |*| Ḃ -⚬ One,
  ): (A |+| B) |*| (Ȧ |&| Ḃ) -⚬ One =
    id                                 [ (A |+| B) |*| (Ȧ |&| Ḃ) ]
      .andThen(matchingChoiceLR)    .to[ (A |*| Ȧ) |+| (B |*| Ḃ) ]
      .either(rInvertA, rInvertB)   .to[           One           ]

  def lInvertChoice[A, B, Ȧ, Ḃ](
    lInvertA: One -⚬ (Ȧ |*| A),
    lInvertB: One -⚬ (Ḃ |*| B),
  ): One -⚬ ((Ȧ |&| Ḃ) |*| (A |+| B)) =
    id                                 [                   One                   ]
      .choice(lInvertA, lInvertB)   .to[ (Ȧ |*|  A       ) |&| (Ḃ |*|        B ) ]
      .in.choiceL.snd.injectL[B]    .to[ (Ȧ |*| (A |+| B)) |&| (Ḃ |*|        B ) ]
      .in.choiceR.snd.injectR[A]    .to[ (Ȧ |*| (A |+| B)) |&| (Ḃ |*| (A |+| B)) ]
      .andThen(coDistributeR)       .to[ (Ȧ |&|                Ḃ) |*| (A |+| B)  ]

  implicit def eitherChoiceDuality[A, B, Ȧ, Ḃ](implicit a: Dual[A, Ȧ], b: Dual[B, Ḃ]): Dual[A |+| B, Ȧ |&| Ḃ] =
    new Dual[A |+| B, Ȧ |&| Ḃ] {
      val rInvert: (A |+| B) |*| (Ȧ |&| Ḃ) -⚬ One =
        rInvertEither(a.rInvert, b.rInvert)

      val lInvert: One -⚬ ((Ȧ |&| Ḃ) |*| (A |+| B)) =
        lInvertChoice(a.lInvert, b.lInvert)
    }

  implicit def choiceEitherDuality[A, B, Ȧ, Ḃ](implicit a: Dual[A, Ȧ], b: Dual[B, Ḃ]): Dual[A |&| B, Ȧ |+| Ḃ] =
    dualSymmetric(eitherChoiceDuality(dualSymmetric(a), dualSymmetric(b)))

  implicit def valNegDuality[A]: Dual[Val[A], Neg[A]] =
    new Dual[Val[A], Neg[A]] {
      val lInvert: One -⚬ (Neg[A] |*| Val[A]) = promise[A]
      val rInvert: (Val[A] |*| Neg[A]) -⚬ One = fulfill[A]
    }

  implicit def negValDuality[A]: Dual[Neg[A], Val[A]] =
    dualSymmetric(valNegDuality)

  implicit def doneNeedDuality: Dual[Done, Need] =
    new Dual[Done, Need] {
      val rInvert: (Done |*| Need) -⚬ One = rInvertSignal
      val lInvert: One -⚬ (Need |*| Done) = lInvertSignal
    }

  /** Evidence that if `A` is dual to `B`, then `F[A]` is dual to `G[B]`. */
  trait Dual1[F[_], G[_]] {
    def rInvert[A, Ā](rInvert: (A |*| Ā) -⚬ One): (F[A] |*| G[Ā]) -⚬ One
    def lInvert[A, Ā](lInvert: One -⚬ (Ā |*| A)): One -⚬ (G[Ā] |*| F[A])

    def apply[A, Ā](ev: Dual[A, Ā]): Dual[F[A], G[Ā]] =
      new Dual[F[A], G[Ā]] {
        val rInvert: (F[A] |*| G[Ā]) -⚬ One = Dual1.this.rInvert(ev.rInvert)
        val lInvert: One -⚬ (G[Ā] |*| F[A]) = Dual1.this.lInvert(ev.lInvert)
      }
  }

  /** If `F[A]` is dual to `G[B]` for all dual pairs `A`, `B`, then `Rec[F]` is dual to `Rec[G]`. */
  def dualRec[F[_], G[_]](ev: Dual1[F, G]): Dual[Rec[F], Rec[G]] =
    new Dual[Rec[F], Rec[G]] {
      val rInvert: (Rec[F] |*| Rec[G]) -⚬ One = rec { self =>
        id                                   [   Rec[F]  |*|   Rec[G]  ]
          .par(unpack[F], unpack[G])      .to[ F[Rec[F]] |*| G[Rec[G]] ]
          .andThen(ev.rInvert(self))      .to[           One           ]
      }

      val lInvert: One -⚬ (Rec[G] |*| Rec[F]) = rec { self =>
        id                                   [           One           ]
          .andThen(ev.lInvert(self))      .to[ G[Rec[G]] |*| F[Rec[F]] ]
          .par(pack[G], pack[F])          .to[   Rec[G]  |*|   Rec[F]  ]
      }
    }

  /** Given `A` and `B` concurrently (`A |*| B`), we can mandate that `A` be consumed before `B`
    * by turning it into `Ā =⚬ B`, where `Ā` is the dual of `A`.
    */
  def unveilSequentially[A, Ā, B](implicit ev: Dual[A, Ā]): (A |*| B) -⚬ (Ā =⚬ B) =
    id[(A |*| B) |*| Ā]           .to[ (A |*|  B) |*| Ā  ]
      .timesAssocLR               .to[  A |*| (B  |*| Ā) ]
      .in.snd(swap)               .to[  A |*| (Ā  |*| B) ]
      .timesAssocRL               .to[ (A |*|  Ā) |*| B  ]
      .elimFst(ev.rInvert)        .to[                B  ]
      .as[ ((A |*| B) |*| Ā) -⚬ B ]
      .curry

  /** Make the function on the left ''"absorb"'' the value on the right and return it as part of its output. */
  def absorbR[A, B, C]: ((A =⚬ B) |*| C) -⚬ (A =⚬ (B |*| C)) =
    id[((A =⚬ B) |*| C) |*| A]  .to[ ((A =⚬ B) |*| C) |*| A ]
      .timesAssocLR             .to[ (A =⚬ B) |*| (C |*| A) ]
      .in.snd(swap)             .to[ (A =⚬ B) |*| (A |*| C) ]
      .timesAssocRL             .to[ ((A =⚬ B) |*| A) |*| C ]
      .in.fst(eval)             .to[        B         |*| C ]
      .as[ (((A =⚬ B) |*| C) |*| A) -⚬ (B |*| C) ]
      .curry

  type Maybe[A] = One |+| A
  object Maybe {
    def empty[A]: One -⚬ Maybe[A] =
      injectL

    def just[A]: A -⚬ Maybe[A] =
      injectR

    def liftOption[A]: Val[Option[A]] -⚬ Maybe[Val[A]] =
      id[Val[Option[A]]]                .to[ Val[Option[      A]] ]
        .andThen(liftV(_.toRight(())))  .to[ Val[Either[Unit, A]] ]
        .andThen(liftEither)            .to[ Val[Unit] |+| Val[A] ]
        .in.left(dsl.discard)           .to[   One     |+| Val[A] ]

    def unliftOption[A]: Maybe[Val[A]] -⚬ Val[Option[A]] =
      id[Maybe[Val[A]]]               .to[    One    |+| Val[A] ]
        .in.left(const_(()))          .to[ Val[Unit] |+| Val[A] ]
        .andThen(unliftEither)        .to[ Val[Either[Unit, A]] ]
        .andThen(liftV(_.toOption))   .to[ Val[Option[A]]       ]

    def getOrElse[A](f: One -⚬ A): Maybe[A] -⚬ A =
      either(f, id)

    def discard[A](f: A -⚬ One): Maybe[A] -⚬ One =
      either(id, f)

    def discard[A](implicit A: Comonoid[A]): Maybe[A] -⚬ One =
      discard(A.counit)
  }

  type PMaybe[A] = Done |+| A
  object PMaybe {
    def empty[A]: Done -⚬ PMaybe[A] =
      injectL

    def just[A]: A -⚬ PMaybe[A] =
      injectR

    def liftOption[A]: Val[Option[A]] -⚬ PMaybe[Val[A]] =
      id[Val[Option[A]]]                .to[ Val[Option[      A]] ]
        .andThen(liftV(_.toRight(())))  .to[ Val[Either[Unit, A]] ]
        .andThen(liftEither)            .to[ Val[Unit] |+| Val[A] ]
        .in.left(dsl.neglect)           .to[   Done    |+| Val[A] ]

    def unliftOption[A]: PMaybe[Val[A]] -⚬ Val[Option[A]] =
      id[PMaybe[Val[A]]]              .to[   Done    |+| Val[A] ]
        .in.left(const(()))           .to[ Val[Unit] |+| Val[A] ]
        .andThen(unliftEither)        .to[ Val[Either[Unit, A]] ]
        .andThen(liftV(_.toOption))   .to[ Val[Option[A]]       ]

    def getOrElse[A](f: Done -⚬ A): PMaybe[A] -⚬ A =
      either(f, id)

    def neglect[A](f: A -⚬ Done): PMaybe[A] -⚬ Done =
      either(id, f)

    def neglect[A](implicit A: PComonoid[A]): PMaybe[A] -⚬ Done =
      neglect(A.counit)
  }

  def parFromOne[A, B](f: One -⚬ A, g: One -⚬ B): One -⚬ (A |*| B) =
    introSnd[One] andThen par(f, g)

  def parToOne[A, B](f: A -⚬ One, g: B -⚬ One): (A |*| B) -⚬ One =
    par(f, g) andThen elimSnd[One]

  type MultipleF[A, X] = One |+| (A |+| (X |*| X))

  /** Zero or more instances of `A`. The exact multiplicity is determined by the producer. */
  type Multiple[A] = Rec[MultipleF[A, *]]
  object Multiple {
    def zero[A]: One -⚬ Multiple[A] =
      id[One]
        .injectL[A |+| (Multiple[A] |*| Multiple[A])]
        .pack[MultipleF[A, *]]

    def one[A]: A -⚬ Multiple[A] =
      id[A]
        .injectL[Multiple[A] |*| Multiple[A]]
        .injectR[One]
        .pack[MultipleF[A, *]]

    def append[A]: (Multiple[A] |*| Multiple[A]) -⚬ Multiple[A] =
      id[Multiple[A] |*| Multiple[A]]
        .injectR[A]
        .injectR[One]
        .pack[MultipleF[A, *]]

    def switch[A, R](
      case0: One -⚬ R,
      case1: A -⚬ R,
      caseN: (Multiple[A] |*| Multiple[A]) -⚬ R,
    ): Multiple[A] -⚬ R =
      unpack[MultipleF[A, *]] andThen either(case0, either(case1, caseN))

    def flatten[A]: Multiple[Multiple[A]] -⚬ Multiple[A] = rec { self =>
      switch(
        case0 = zero,
        case1 = id,
        caseN = par(self, self) andThen append
      )
    }
  }

  type UnlimitedF[A, X] = One |&| (A |&| (X |*| X))

  /** Unlimited supply of `A`s. The consumer chooses how many `A`s to consume. */
  type Unlimited[A] = Rec[UnlimitedF[A, *]]
  object Unlimited {
    def discard[A]: Unlimited[A] -⚬ One =
      unpack[UnlimitedF[A, *]] andThen chooseL

    def single[A]: Unlimited[A] -⚬ A =
      unpack[UnlimitedF[A, *]] andThen chooseR andThen chooseL

    def double[A]: Unlimited[A] -⚬ (Unlimited[A] |*| Unlimited[A]) =
      unpack[UnlimitedF[A, *]] andThen chooseR andThen chooseR

    def create[X, A](
      case0: X -⚬ One,
      case1: X -⚬ A,
      caseN: X -⚬ (Unlimited[A] |*| Unlimited[A]),
    ): X -⚬ Unlimited[A] =
      choice(case0, choice(case1, caseN)) andThen pack[UnlimitedF[A, *]]

    def duplicate[A]: Unlimited[A] -⚬ Unlimited[Unlimited[A]] = rec { self =>
      create(
        case0 = discard,
        case1 = id,
        caseN = double andThen par(self, self)
      )
    }
  }

  type PUnlimitedF[A, X] = Done |&| (A |&| (X |*| X))
  type PUnlimited[A] = Rec[PUnlimitedF[A, *]]
  object PUnlimited {
    def neglect[A]: PUnlimited[A] -⚬ Done =
      unpack[PUnlimitedF[A, *]] andThen chooseL

    def single[A]: PUnlimited[A] -⚬ A =
      unpack[PUnlimitedF[A, *]] andThen chooseR andThen chooseL

    def double[A]: PUnlimited[A] -⚬ (PUnlimited[A] |*| PUnlimited[A]) =
      unpack[PUnlimitedF[A, *]] andThen chooseR andThen chooseR

    def create[X, A](
      case0: X -⚬ Done,
      case1: X -⚬ A,
      caseN: X -⚬ (PUnlimited[A] |*| PUnlimited[A]),
    ): X -⚬ PUnlimited[A] =
      choice(case0, choice(case1, caseN)) andThen pack[PUnlimitedF[A, *]]

    def duplicate[A]: PUnlimited[A] -⚬ PUnlimited[PUnlimited[A]] = rec { self =>
      create(
        case0 = neglect,
        case1 = id,
        caseN = double andThen par(self, self)
      )
    }
  }

  trait Semigroup[A] {
    def combine: (A |*| A) -⚬ A

    def law_associativity: Equal[ ((A |*| A) |*| A) -⚬ A ] =
      Equal(
        par(combine, id[A]) >>> combine,
        timesAssocLR >>> par(id[A], combine) >>> combine,
      )
  }

  trait Cosemigroup[A] {
    def split: A -⚬ (A |*| A)

    def law_coAssociativity: Equal[ A -⚬ ((A |*| A) |*| A) ] =
      Equal(
        split >>> par(split, id[A]),
        split >>> par(id[A], split) >>> timesAssocRL,
      )
  }

  trait Monoid[A] extends Semigroup[A] {
    def unit: One -⚬ A

    def law_leftUnit: Equal[ (One |*| A) -⚬ A ] =
      Equal(
        par(unit, id[A]) >>> combine,
        elimFst,
      )

    def law_rightUnit: Equal[ (A |*| One) -⚬ A ] =
      Equal(
        par(id[A], unit) >>> combine,
        elimSnd,
      )
  }

  trait Comonoid[A] extends Cosemigroup[A] {
    def counit: A -⚬ One

    def law_leftCounit: Equal[ A -⚬ (One |*| A) ] =
      Equal(
        split >>> par(counit, id[A]),
        introFst,
      )

    def law_rightCounit: Equal[ A -⚬ (A |*| One) ] =
      Equal(
        split >>> par(id[A], counit),
        introSnd,
      )
  }

  /** A [[Monoid]] whose [[unit]] can be chained after a signal flowing in the '''P'''ositive direction ([[Done]]),
    * effectively taking on the responsibility to wait for completion of some computation.
    *
    * Its dual is [[NComonoid]].
    */
  trait PMonoid[A] extends Semigroup[A] {
    def unit: Done -⚬ A

    def monoid: Monoid[A] = new Monoid[A] {
      def combine: (A |*| A) -⚬ A = PMonoid.this.combine
      def unit: One -⚬ A = done >>> PMonoid.this.unit
    }

    def law_leftUnit: Equal[ (One |*| A) -⚬ A ] =
      Equal(
        par(done >>> unit, id[A]) >>> combine,
        elimFst,
      )

    def law_rightUnit: Equal[ (A |*| One) -⚬ A ] =
      Equal(
        par(id[A], done >>> unit) >>> combine,
        elimSnd,
      )
  }

  /** A [[Comonoid]] whose [[counit]] can be chained before a signal flowing in the '''N'''egative direction ([[Need]]),
    * effectively taking on the responsibility to await completion of some computation.
    *
    * The dual of [[PMonoid]].
    */
  trait NComonoid[A] extends Cosemigroup[A] {
    def counit: A -⚬ Need

    def comonoid: Comonoid[A] = new Comonoid[A] {
      def split: A -⚬ (A |*| A) = NComonoid.this.split
      def counit: A -⚬ One = NComonoid.this.counit >>> need
    }

    def law_leftCounit: Equal[ A -⚬ (One |*| A) ] =
      Equal(
        split >>> par(counit >>> need, id[A]),
        introFst,
      )

    def law_rightCounit: Equal[ A -⚬ (A |*| One) ] =
      Equal(
        split >>> par(id[A], counit >>> need),
        introSnd,
      )
  }

  /** A weaker version of [[Monoid]] whose [[unit]] creates a liability - a signal traveling in the '''N'''egative
    * direction ([[Need]]) that eventually needs to be awaited.
    *
    * Its dual is [[PComonoid]].
    */
  trait NMonoid[A] extends Semigroup[A] {
    def unit: Need -⚬ A

    def law_leftUnit: Equal[ (Need |*| A) -⚬ A ] =
      Equal(
        par(regressInfinitely >>> unit, id[A]) >>> combine,
        id[Need |*| A].elimFst(regressInfinitely >>> need),
      )

    def law_rightUnit: Equal[ (A |*| Need) -⚬ A ] =
      Equal(
        par(id[A], regressInfinitely >>> unit) >>> combine,
        id[A |*| Need].elimSnd(regressInfinitely >>> need),
      )
  }

  /** A weaker version of [[Comonoid]] whose [[counit]] cannot discard the input completely, but can reduce it to
    * a signal traveling in the '''P'''ositive direction ([[Done]]) that eventually needs to be awaited.
    *
    * The dual of [[NMonoid]].
    */
  trait PComonoid[A] extends Cosemigroup[A] {
    def counit: A -⚬ Done

    def law_leftCounit: Equal[ A -⚬ (Done |*| A) ] =
      Equal(
        split >>> par(counit >>> delayIndefinitely, id[A]),
        id[A].introFst(done >>> delayIndefinitely),
      )

    def law_rightCounit: Equal[ A -⚬ (A |*| Done) ] =
      Equal(
        split >>> par(id[A], counit >>> delayIndefinitely),
        id[A].introSnd(done >>> delayIndefinitely),
      )
  }

  trait Monad[F[_]] {
    def pure[A]    :       A -⚬ F[A]
    def flatten[A] : F[F[A]] -⚬ F[A]
  }

  trait Comonad[F[_]] {
    def extract[A]   : F[A] -⚬ A
    def duplicate[A] : F[A] -⚬ F[F[A]]
  }

  @deprecated("uses deprecated discard")
  implicit def comonoidVal[A]: Comonoid[Val[A]] =
    new Comonoid[Val[A]] {
      def counit : Val[A] -⚬ One                 = discard
      def split  : Val[A] -⚬ (Val[A] |*| Val[A]) = dup
    }

  implicit def pComonoidVal[A]: PComonoid[Val[A]] =
    new PComonoid[Val[A]] {
      def counit : Val[A] -⚬ Done                = neglect
      def split  : Val[A] -⚬ (Val[A] |*| Val[A]) = dup
    }

  @deprecated("uses deprecated fakeDemand")
  implicit def monoidNeg[A]: Monoid[Neg[A]] =
    new Monoid[Neg[A]] {
      def unit    :                 One -⚬ Neg[A] = fakeDemand
      def combine : (Neg[A] |*| Neg[A]) -⚬ Neg[A] = mergeDemands
    }

  implicit def nMonoidNeg[A]: NMonoid[Neg[A]] =
    new NMonoid[Neg[A]] {
      def unit    :                Need -⚬ Neg[A] = inflate
      def combine : (Neg[A] |*| Neg[A]) -⚬ Neg[A] = mergeDemands
    }

  implicit def monoidMultiple[A]: Monoid[Multiple[A]] =
    new Monoid[Multiple[A]] {
      def unit    :                           One -⚬ Multiple[A] = Multiple.zero
      def combine : (Multiple[A] |*| Multiple[A]) -⚬ Multiple[A] = Multiple.append
    }

  implicit val monadMultiple: Monad[Multiple] =
    new Monad[Multiple] {
      def pure[A]    :                     A -⚬ Multiple[A] = Multiple.one
      def flatten[A] : Multiple[Multiple[A]] -⚬ Multiple[A] = Multiple.flatten
    }

  implicit def comonoidUnlimited[A]: Comonoid[Unlimited[A]] =
    new Comonoid[Unlimited[A]] {
      def counit : Unlimited[A] -⚬ One                             = Unlimited.discard
      def split  : Unlimited[A] -⚬ (Unlimited[A] |*| Unlimited[A]) = Unlimited.double
    }

  implicit def pComonoidPUnlimited[A]: PComonoid[PUnlimited[A]] =
    new PComonoid[PUnlimited[A]] {
      def counit : PUnlimited[A] -⚬ Done                              = PUnlimited.neglect
      def split  : PUnlimited[A] -⚬ (PUnlimited[A] |*| PUnlimited[A]) = PUnlimited.double
    }

  implicit val comonadUnlimited: Comonad[Unlimited] =
    new Comonad[Unlimited] {
      def extract[A]   : Unlimited[A] -⚬ A                       = Unlimited.single
      def duplicate[A] : Unlimited[A] -⚬ Unlimited[Unlimited[A]] = Unlimited.duplicate
    }

  implicit val comonadPUnlimited: Comonad[PUnlimited] =
    new Comonad[PUnlimited] {
      def extract[A]   : PUnlimited[A] -⚬ A                         = PUnlimited.single
      def duplicate[A] : PUnlimited[A] -⚬ PUnlimited[PUnlimited[A]] = PUnlimited.duplicate
    }

  def getFst[A, B](implicit A: Cosemigroup[A]): (A |*| B) -⚬ (A |*| (A |*| B)) =
    id                             [     A     |*| B  ]
      .in.fst(A.split)          .to[ (A |*| A) |*| B  ]
      .timesAssocLR             .to[  A |*| (A |*| B) ]

  def getSnd[A, B](implicit B: Cosemigroup[B]): (A |*| B) -⚬ (B |*| (A |*| B)) =
    id                             [  A |*|     B     ]
      .in.snd(B.split)          .to[  A |*| (B |*| B) ]
      .timesAssocRL             .to[ (A |*| B) |*| B  ]
      .swap                     .to[  B |*| (A |*| B) ]

  def discardFst[A, B](implicit A: Comonoid[A]): (A |*| B) -⚬ B =
    id                             [  A  |*| B ]
      .elimFst(A.counit)        .to[         B ]

  def discardSnd[A, B](implicit B: Comonoid[B]): (A |*| B) -⚬ A =
    id                             [ A |*|  B  ]
      .elimSnd(B.counit)        .to[ A         ]
}
