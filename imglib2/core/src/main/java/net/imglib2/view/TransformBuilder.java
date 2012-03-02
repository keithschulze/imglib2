package net.imglib2.view;

import java.util.LinkedList;
import java.util.ListIterator;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.Transform;
import net.imglib2.transform.integer.BoundingBox;
import net.imglib2.transform.integer.BoundingBoxTransform;
import net.imglib2.transform.integer.Mixed;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.transform.integer.SlicingTransform;
import net.imglib2.transform.integer.TranslationTransform;
import net.imglib2.util.Util;

public class TransformBuilder< T >
{
	/**
	 * Get a RandomAccessible which provides RandomAccess to the specified
	 * {@code interval} of {@code randomAccessible}.
	 *
	 * <p>
	 * Create a new TransformBuilder that traverses the view hierarchy starting
	 * from {@code randomAccessible}. {@link #build()} an efficient
	 * RandomAccessible by joining and simplifying the collected
	 * transformations.
	 * </p>
	 *
	 * @param interval
	 *            The interval in which access is needed.
	 * @param randomAccessible
	 */
	public static < S > RandomAccessible< S > getEfficientRandomAccessible( final Interval interval, final RandomAccessible< S > randomAccessible )
	{
		return new TransformBuilder< S >( interval, randomAccessible ).build();
	}

	/**
	 * Provides the untransformed random access.
	 */
	RandomAccessible< T > source;

	/**
	 * Interval transformed to the currently visited view. null means that the
	 * interval is infinite.
	 */
	BoundingBox boundingBox;

	/**
	 * List of transforms that have to be applied when wrapping the
	 * {@link #source} RandomAccess to obtain a RandomAccess in the target
	 * coordinate system.
	 */
	LinkedList< Transform > transforms;

	/**
	 * Create a new TransformBuilder. Starting from {@code randomAccessible}, go
	 * down the view hierarchy to the RandomAccessible that will provide the
	 * source RandomAccess into the specified {@code interval}. While traversing
	 * the view hierarchy transforms are collected into the {@link #transforms}
	 * list. These transforms have to be applied when wrapping the source
	 * RandomAccess to obtain a RandomAccess in the coordinate system of
	 * {@code randomAccessible}.
	 *
	 * @param interval
	 *            The interval in which access is needed. This is converted to a
	 *            bounding box which is propagated through the transforms down
	 *            the view hierarchy.
	 * @param randomAccessible
	 */
	TransformBuilder( final Interval interval, final RandomAccessible< T > randomAccessible )
	{
		transforms = new LinkedList< Transform >();
		boundingBox = ( interval == null) ? null : new BoundingBox( interval );
		// System.out.println( randomAccessible );
		visit( randomAccessible );
	}

	/**
	 * Prepend a transform to the {@link #transforms} list. Also apply the
	 * transform to {@link #boundingBox}, which will be used to specify the
	 * interval for the RandomAccess on the final source (at the end of the view
	 * chain). This is called while traversing the view hierarchy.
	 *
	 * @param t
	 *            the transform to add.
	 */
	protected void prependTransform( final Transform t )
	{
		if ( BoundingBoxTransform.class.isInstance( t ) && ( boundingBox != null ) )
			boundingBox = ( ( BoundingBoxTransform ) t ).transform( boundingBox );
		else
			boundingBox = null;
		transforms.addFirst( t );
	}

	/**
	 * Visit a RandomAccessible (while traversing the view hierarchy). The
	 * {@code randomAccessible} is handled by
	 * {@link #visitTransformed(TransformedRandomAccessible)} or
	 * {@link #visitExtended(ExtendedRandomAccessibleInterval)} when it has the
	 * appropriate type. Otherwise, the traversal stops and
	 * {@code randomAccessible} is set as the {@link #source}.
	 *
	 * @param randomAccessible
	 */
	@SuppressWarnings( "unchecked" )
	protected void visit( final RandomAccessible< T > randomAccessible )
	{
		if ( TransformedRandomAccessible.class.isInstance( randomAccessible ) )
		{
			visitTransformed( ( TransformedRandomAccessible< T > ) randomAccessible );
		}
		else if ( ExtendedRandomAccessibleInterval.class.isInstance( randomAccessible ) )
		{
			visitExtended( ( ExtendedRandomAccessibleInterval< T, ? > ) randomAccessible );
		}
		else if ( IntervalView.class.isInstance( randomAccessible ) )
		{
			visit( ( ( IntervalView< T > ) randomAccessible ).getSource() );
		}
		else
		{
			source = randomAccessible;
		}
	}

	/**
	 * Visit a TransformedRandomAccessible (while traversing the view
	 * hierarchy). Append the view's transform to the list and
	 * {@link #visit(RandomAccessible)} the view's source.
	 *
	 * @param randomAccessible
	 */
	protected void visitTransformed( final TransformedRandomAccessible< T > randomAccessible )
	{
		prependTransform( randomAccessible.getTransformToSource() );
		visit( randomAccessible.getSource() );
	}

	/**
	 * Visit a ExtendedRandomAccessibleInterval (while traversing the view
	 * hierarchy). If the no out-of-bounds extension is needed for the current
	 * bounding box, {@link #visit(RandomAccessible)} the view's source.
	 * Otherwise, the traversal stops and {@code randomAccessible} is set as the
	 * {@link #source}.
	 *
	 * @param randomAccessible
	 */
	protected void visitExtended( final ExtendedRandomAccessibleInterval< T, ? > randomAccessible )
	{
		final RandomAccessibleInterval< T > sourceInterval = randomAccessible.getSource();
		if ( ( boundingBox != null ) && Util.contains( sourceInterval, boundingBox.getInterval() ) )
			visit( sourceInterval );
		else
			source = randomAccessible;
	}

	public static boolean isIdentity( final Mixed t )
	{
		final int n = t.numSourceDimensions();
		final int m = t.numTargetDimensions();
		if ( n != m )
			return false;

		for ( int d = 0; d < m; ++d )
		{
			if ( t.getTranslation( d ) != 0 )
				return false;
			if ( t.getComponentZero( d ) )
				return false;
			if ( t.getComponentInversion( d ) )
				return false;
			if ( t.getComponentMapping( d ) != d )
				return false;
		}
		return true;
	}

	public static boolean isTranslation( final Mixed t )
	{
		final int n = t.numSourceDimensions();
		final int m = t.numTargetDimensions();
		if ( n != m )
			return false;

		for ( int d = 0; d < m; ++d )
		{
			if ( t.getComponentZero( d ) )
				return false;
			if ( t.getComponentInversion( d ) )
				return false;
			if ( t.getComponentMapping( d ) != d )
				return false;
		}
		return true;
	}

	public static boolean isComponentMapping( final Mixed t )
	{
		final int m = t.numTargetDimensions();

		for ( int d = 0; d < m; ++d )
		{
			if ( t.getTranslation( d ) != 0 )
				return false;
			if ( t.getComponentZero( d ) )
				return false;
			if ( t.getComponentInversion( d ) )
				return false;
		}
		return true;
	}

	public static boolean isSlicing( final Mixed t )
	{
		final int n = t.numSourceDimensions();
		final int m = t.numTargetDimensions();
		if ( n > m )
			return false;

		for ( int d = 0; d < m; ++d )
		{
			if ( t.getTranslation( d ) != 0 && ( ! t.getComponentZero( d ) ) )
				return false;
			if ( t.getComponentInversion( d ) )
				return false;
		}
		return true;
	}

	/**
	 * Simplify the transforms list and create a sequence of wrapped
	 * RandomAccessibles.
	 *
	 * @return RandomAccessible on the interval specified in the constructor.
	 */
	protected RandomAccessible< T > build()
	{
		net.imglib2.concatenate.Util.join( transforms );

		// TODO: simplify transform list
		for ( final ListIterator< Transform > i = transforms.listIterator(); i.hasNext(); )
		{
			final Transform t = i.next();
			if ( Mixed.class.isInstance( t ) )
			{
				final Mixed mixed = ( Mixed ) t;
				if ( isIdentity( mixed ) )
				{
					// found identity
					// remove from transforms list
					i.remove();
				}
				else if ( isTranslation( mixed ) )
				{
					// found pure translation
					// replace by a TranslationTransform
					final long[] translation = new long[ mixed.numTargetDimensions() ];
					mixed.getTranslation( translation );
					i.set( new TranslationTransform( translation ) );
				}
//				else if ( isComponentMapping( mixed ) )
//				{
//					// found pure component mapping
//					// replace by a ComponentMappingTransform
//					final int[] component = new int[ mixed.numTargetDimensions() ];
//					mixed.getComponentMapping( component );
//					i.set( new ComponentMappingTransform( component ) );
//				}
				else if ( isSlicing ( mixed ) )
				{
					// found pure slicing
					// replace by a SlicingTransform
					final int m = mixed.numTargetDimensions();
					final long[] translation = new long[ m ];
					final boolean[] zero = new boolean[ m ];
					final int[] component = new int[ m ];
					mixed.getTranslation( translation );
					mixed.getComponentZero( zero );
					mixed.getComponentMapping( component );
					final SlicingTransform sl = new SlicingTransform( mixed.numSourceDimensions(), m );
					sl.setTranslation( translation );
					sl.setComponentZero( zero );
					sl.setComponentMapping( component );
					i.set( sl );
				}
			}
		}

		// build RandomAccessibles
		RandomAccessible< T > result = source;
		for ( final ListIterator< Transform > i = transforms.listIterator(); i.hasNext(); )
		{
			final Transform t = i.next();
			if ( MixedTransform.class.isInstance( t ) )
				result = wrapMixedTransform( result, ( MixedTransform ) t );
			else if ( TranslationTransform.class.isInstance( t ) )
				result = wrapTranslationTransform( result, ( TranslationTransform ) t );
			else if ( SlicingTransform.class.isInstance( t ) )
				result = wrapSlicingTransform( result, ( SlicingTransform ) t );
			else
				result = wrapGenericTransform( result, t );
		}
		return result;
	}

	protected RandomAccessible< T > wrapGenericTransform( final RandomAccessible< T > s, final Transform t )
	{
		return new RandomAccessible< T >()
		{
			@Override
			public int numDimensions()
			{
				return t.numSourceDimensions();
			}

			@Override
			public RandomAccess< T > randomAccess()
			{
				return new TransformRandomAccess< T >( s.randomAccess(), t );
			}

			@Override
			public RandomAccess< T > randomAccess( final Interval interval )
			{
				return new TransformRandomAccess< T >( s.randomAccess(), t );
			}
		};
	}

	protected RandomAccessible< T > wrapMixedTransform( final RandomAccessible< T > s, final MixedTransform t )
	{
		final boolean full = t.hasFullSourceMapping();
		return new RandomAccessible< T >()
		{
			@Override
			public int numDimensions()
			{
				return t.numSourceDimensions();
			}

			@Override
			public RandomAccess< T > randomAccess()
			{
				if ( full )
					return new FullSourceMapMixedRandomAccess< T >( s.randomAccess(), t );
				else
					return new MixedRandomAccess< T >( s.randomAccess(), t );
			}

			@Override
			public RandomAccess< T > randomAccess( final Interval interval )
			{
				if ( full )
					return new FullSourceMapMixedRandomAccess< T >( s.randomAccess(), t );
				else
					return new MixedRandomAccess< T >( s.randomAccess(), t );
			}
		};
	}

	protected RandomAccessible< T > wrapTranslationTransform( final RandomAccessible< T > s, final TranslationTransform t )
	{
		return new RandomAccessible< T >()
		{
			@Override
			public int numDimensions()
			{
				return t.numSourceDimensions();
			}

			@Override
			public TranslationRandomAccess< T > randomAccess()
			{
				return new TranslationRandomAccess< T >( s.randomAccess(), t );
			}

			@Override
			public TranslationRandomAccess< T > randomAccess( final Interval interval )
			{
				return new TranslationRandomAccess< T >( s.randomAccess(), t );
			}
		};
	}

	protected RandomAccessible< T > wrapSlicingTransform( final RandomAccessible< T > s, final SlicingTransform t )
	{
		return new RandomAccessible< T >()
		{
			@Override
			public int numDimensions()
			{
				return t.numSourceDimensions();
			}

			@Override
			public SlicingRandomAccess< T > randomAccess()
			{
				return new SlicingRandomAccess< T >( s.randomAccess(), t );
			}

			@Override
			public SlicingRandomAccess< T > randomAccess( final Interval interval )
			{
				return new SlicingRandomAccess< T >( s.randomAccess(), t );
			}
		};
	}
}
