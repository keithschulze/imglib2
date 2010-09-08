/**
 * Copyright (c) 2009--2010, Stephan Preibisch & Stephan Saalfeld
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.  Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution.  Neither the name of the Fiji project nor
 * the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package mpicbg.imglib.sampler.array;

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.sampler.AbstractBasicRasterIterator;
import mpicbg.imglib.type.Type;

/**
 * 
 * @param <T>
 *
 * @author Stephan Preibisch and Stephan Saalfeld
 */
public class ArrayBasicRasterIterator< T extends Type< T > > extends AbstractBasicRasterIterator< T >
{
	protected final T type;
	protected final Array< T, ? > container;
	protected final int sizeMinus1;
	
	public ArrayBasicRasterIterator( final Array< T, ? > container, final Image< T > image ) 
	{
		super( container, image );

		this.type = container.createLinkedType();
		this.container = container;
		this.sizeMinus1 = (int)container.numPixels() - 1;
		
		reset();
	}
	
	@Override
	public T type() { return type; }
	
	@Override
	public boolean hasNext(){ return type.getIndex() < sizeMinus1; }

	@Override
	public void jumpFwd( final long steps )
	{
		type.incIndex( (int)steps );
		
		linkedIterator.jumpFwd( steps );
	}

	@Override
	public void fwd()
	{
		type.incIndex();
		
		linkedIterator.fwd();
	}

	@Override
	public void close() 
	{ 
		type.updateIndex( sizeMinus1 + 1 );
		super.close();
	}

	@Override
	public void reset()
	{ 
		type.updateIndex( -1 ); 
		type.updateContainer( this );

		linkedIterator.reset();
	}

	@Override
	public Array<T,?> getContainer(){ return container; }
	
	@Override
	public String toString() { return type.toString(); }

	@Override
	public long getLongPosition( final int dim )
	{
		return container.indexToPosition( type.getIndex(), dim );
	}

	@Override
	public void localize( final long[] position )
	{
		container.indexToPosition( type.getIndex(), position );
	}
}
