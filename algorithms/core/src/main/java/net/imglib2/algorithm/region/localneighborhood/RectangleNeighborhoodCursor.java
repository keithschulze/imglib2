/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package net.imglib2.algorithm.region.localneighborhood;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.IntervalIndexer;

public final class RectangleNeighborhoodCursor< T > extends RectangleNeighborhoodLocalizableSampler< T > implements Cursor< Neighborhood< T > >
{
	private final long[] dimensions;

	private final long[] min;

	private final long[] max;

	private long index;

	private final long maxIndex;

	private long maxIndexOnLine;

	public RectangleNeighborhoodCursor( final RandomAccessibleInterval< T > source, final Interval span, final RectangleNeighborhoodFactory< T > factory )
	{
		super( source, span, factory, source );

		dimensions = new long[ n ];
		min = new long[ n ];
		max = new long[ n ];
		source.dimensions( dimensions );
		source.min( min );
		source.max( max );
		long size = dimensions[ 0 ];
		for ( int d = 1; d < n; ++d )
			size *= dimensions[ d ];
		maxIndex = size;
		reset();
	}

	private RectangleNeighborhoodCursor( final RectangleNeighborhoodCursor< T > c )
	{
		super( c );
		dimensions = c.dimensions.clone();
		min = c.min.clone();
		max = c.max.clone();
		maxIndex = c.maxIndex;
		index = c.index;
		maxIndexOnLine = c.maxIndexOnLine;
	}

	@Override
	public void fwd()
	{
		++currentPos[ 0 ];
		++currentMin[ 0 ];
		++currentMax[ 0 ];
		if ( ++index > maxIndexOnLine )
			nextLine();
	}

	private void nextLine()
	{
		currentPos[ 0 ] = min[ 0 ];
		currentMin[ 0 ] -= dimensions[ 0 ];
		currentMax[ 0 ] -= dimensions[ 0 ];
		maxIndexOnLine += dimensions[ 0 ];
		for ( int d = 1; d < n; ++d )
		{
			++currentPos[ d ];
			++currentMin[ d ];
			++currentMax[ d ];
			if ( currentPos[ d ] > max[ d ] )
			{
				currentPos[ d ] = min[ d ];
				currentMin[ d ] -= dimensions[ d ];
				currentMax[ d ] -= dimensions[ d ];
			}
			else
				break;
		}
	}

	@Override
	public void reset()
	{
		index = 0;
		maxIndexOnLine = dimensions[ 0 ];
		for ( int d = 0; d < n; ++d )
		{
			currentPos[ d ] = ( d == 0 ) ? min[ d ] - 1 : min[ d ];
			currentMin[ d ] = currentPos[ d ] + span.min( d );
			currentMax[ d ] = currentPos[ d ] + span.max( d );
		}
	}

	@Override
	public boolean hasNext()
	{
		return index < maxIndex;
	}

	@Override
	public void jumpFwd( final long steps )
	{
		index += steps;
		maxIndexOnLine = ( index < 0 ) ? dimensions[ 0 ] : ( 1 + index / dimensions[ 0 ] ) * dimensions[ 0 ];
		IntervalIndexer.indexToPositionWithOffset( index + 1, dimensions, min, currentPos );
		for ( int d = 0; d < n; ++d )
		{
			currentMin[ d ] = currentPos[ d ] + span.min( d );
			currentMax[ d ] = currentPos[ d ] + span.max( d );
		}
	}

	@Override
	public Neighborhood< T > next()
	{
		fwd();
		return get();
	}

	@Override
	public void remove()
	{
		// NB: no action.
	}

	@Override
	public RectangleNeighborhoodCursor< T > copy()
	{
		return new RectangleNeighborhoodCursor< T >( this );
	}

	@Override
	public RectangleNeighborhoodCursor< T > copyCursor()
	{
		return copy();
	}
}
