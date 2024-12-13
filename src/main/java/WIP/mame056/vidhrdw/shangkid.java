/* vidhrdw/shangkid */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.tilemapH.*;
import static mame056.tilemapC.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
import static mame056.memory.*;
import static mame056.memoryH.*;

public class shangkid
{
	
	public static struct_tilemap background;
	public static UBytePtr shangkid_videoreg = new UBytePtr();
	public static int shangkid_gfx_type;
	
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int attributes = videoram.read(tile_index+0x800);
		int tile_number = videoram.read(tile_index)+0x100*(attributes&0x3);
		int color;
	
		if( shangkid_gfx_type==1 )
		{
			/* Shanghai Kid:
				------xx	bank
				-----x--	flipx
				xxxxx---	color
			*/
			color = attributes>>3;
			color = (color&0x03)|((color&0x1c)<<1);
			SET_TILE_INFO(
					0,
					tile_number,
					color,
					(attributes&0x04)!=0?TILE_FLIPX:0);
		}
		else
		{
			/* Chinese Hero:
				------xx	bank
				-xxxxx--	color
				x-------	flipx?
			*/
			color = (attributes>>2)&0x1f;
			SET_TILE_INFO(
					0,
					tile_number,
					color,
					(attributes&0x80)!=0?TILE_FLIPX:0);
		}
	
		tile_info.priority =
			(memory_region( REGION_PROMS ).read(0x800+color*4)==2)?1:0;
            }
        };
	
	public static VhStartPtr shangkid_vh_start = new VhStartPtr() { public int handler() 
	{
		background = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		return background!=null?0:1;
	} };
	
	public static VhStopPtr shangkid_vh_stop = new VhStopPtr() { public void handler() 
	{
	} };
	
	public static WriteHandlerPtr shangkid_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( videoram.read(offset)!=data ){
			videoram.write(offset,data);
			tilemap_mark_tile_dirty( background, offset&0x7ff );
		}
	} };
	
	static void draw_sprite( UBytePtr source, mame_bitmap bitmap ){
		rectangle clip = new rectangle(Machine.visible_area);
		GfxElement gfx;
		int transparent_pen;
		int bank_index;
		int c,r;
		int width,height;
		int sx,sy;
	
		int ypos		= 209 - source.read(0);
		int tile		= source.read(1)&0x3f;
		int xflip		= (source.read(1)&0x40)!=0?1:0;
		int yflip		= (source.read(1)&0x80)!=0?1:0;
		int bank		= source.read(2)&0x3f;
		int xsize		= (source.read(2)&0x40)!=0?1:0;
		int ysize		= (source.read(2)&0x80)!=0?1:0;
		int yscale		= source.read(3)&0x07;	/* 0x0 = smallest; 0x7 = biggest */
		int xpos		= ((source.read(4)+source.read(5)*255)&0x1ff)-23;
		int color		= source.read(6)&0x3f;
		int xscale		= source.read(7)&0x07;	/* 0x0 = smallest; 0x7 = biggest */
	
		/* adjust placement for small sprites */
		if( xsize==0 && xflip!=0 ) xpos -= 16;
		if( ysize==0 && yflip==0 ) ypos += 16;
	
		if( shangkid_gfx_type == 1 )
		{
			/* Shanghai Kid */
			switch( bank&0x30 )
			{
			case 0x00:
			case 0x10:
				tile += 0x40*(bank&0xf);
				break;
	
			case 0x20:
				tile += 0x40*((bank&0x3)|0x10);
				break;
	
			case 0x30:
				tile += 0x40*((bank&0x3)|0x14);
				break;
			}
			bank_index = 0;
			transparent_pen = 3;
		}
		else
		{
			/* Chinese Hero */
			color >>= 1;
			switch( bank>>2 )
			{
			case 0x0: bank_index = 0; break;
			case 0x9: bank_index = 1; break;
			case 0x6: bank_index = 2; break;
			case 0xf: bank_index = 3; break;
			default:
				bank_index = 0;
				break;
			}
	
			if(( bank&0x01 ) != 0) tile += 0x40;
			transparent_pen = 7;
		}
	
		gfx = Machine.gfx[1+bank_index];
	
		width = (xscale+1)*2;
		height = (yscale+1)*2;
	
		/* center zoomed sprites */
		xpos += (16-width)*(xsize+1)/2;
		ypos += (16-height)*(ysize+1)/2;
	
		for( r=0; r<=ysize; r++ )
		{
			for( c=0; c<=xsize; c++ )
			{
				sx = xpos+(c^xflip)*width;
				sy = ypos+(r^yflip)*height;
				drawgfxzoom(
					bitmap,
					gfx,
					tile+c*8+r,
					color,
					xflip,yflip,
					sx,sy,
					clip,
					TRANSPARENCY_PEN,transparent_pen,
					(width<<16)/16, (height<<16)/16 );
			}
		}
	}
	
	static void draw_sprites( mame_bitmap bitmap )
	{
		UBytePtr source=new UBytePtr(), finish=new UBytePtr();
	
		finish = new UBytePtr(spriteram);
		source = new UBytePtr(spriteram, 0x200);
		while( source.offset>finish.offset ){
			source.dec(8);
			draw_sprite( source, bitmap );
		}
	}
	
	public static VhUpdatePtr shangkid_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
            
		int flipscreen = shangkid_videoreg.read(1)&0x80;
		tilemap_set_flip( background, flipscreen!=0?(TILEMAP_FLIPX|TILEMAP_FLIPY):0 );
		tilemap_set_scrollx( background,0,shangkid_videoreg.read(0)-40 );
		tilemap_set_scrolly( background,0,shangkid_videoreg.read(2)+0x10 );
	
		tilemap_draw( bitmap,background,0,0 );
		draw_sprites( bitmap );
		tilemap_draw( bitmap,background,1,0 ); /* high priority tiles */
            }
        };
	
	
	
	
	static void dynamski_draw_background( mame_bitmap bitmap, int pri )
	{
		int i;
		int sx,sy;
		int tile;
		int attr;
		int temp;
		rectangle clip = new rectangle(Machine.visible_area);
	
		int transparency = pri!=0?TRANSPARENCY_PEN:TRANSPARENCY_NONE;
	
		for( i=0; i<0x400; i++ )
		{
			sx = (i%32)*8;
			sy = (i/32)*8;
	
			if( sy<16 )
			{
				temp = sx;
				sx = sy+256+16;
				sy = temp;
			}
			else if( sy>=256-16 )
			{
				temp = sx;
				sx = sy-256+16;
				sy = temp;
			}
			else
			{
				sx+=16;
			}
	
			tile = videoram.read(i);
			attr = videoram.read(i+0x400);
			/*
				x---.----	priority?
				-xx-.----	bank
			*/
			if( pri==0 || (attr>>7)==pri )
			{
				tile += ((attr>>5)&0x3)*256;
				drawgfx(
					bitmap,
					Machine.gfx[0],
					tile,
					0, /* color */
					0,0,//xflip,yflip,
					sx,sy,
					clip,
					transparency,3 );
			}
		}
	}
	
	static void dynamski_draw_sprites( mame_bitmap bitmap )
	{
		int i;
		int sx,sy;
		int tile;
		int bank;
		int attr;
		int color;
		rectangle clip = new rectangle(Machine.visible_area);
		for( i=0x7e; i>=0x00; i-=2 )
		{
			bank = videoram.read(0x1b80+i);
			attr = videoram.read(0x1b81+i);
			tile = videoram.read(0xb80+i);
			color = videoram.read(0xb81+i);
			sy = 240-videoram.read(0x1380+i);
	
			sx = videoram.read(0x1381+i)-64+8+16;
			if(( attr&1 )!=0) sx += 0x100;
	
			drawgfx(
					bitmap,
					Machine.gfx[1],
					bank*0x40 + (tile&0x3f),
					color,
					tile&0x80,tile&0x40, /* flipx,flipy */
					sx,sy,
					clip,
					TRANSPARENCY_PEN,3 );
		}
	}
	
	public static VhUpdatePtr dynamski_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
		dynamski_draw_background( bitmap, 0 );
		dynamski_draw_sprites( bitmap );
		dynamski_draw_background( bitmap, 1 );
            }
        };
}
