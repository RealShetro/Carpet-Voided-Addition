package redstone.multimeter.common;

import com.google.common.base.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import redstone.multimeter.util.AxisUtils;

public class DimPos {

	private final String dimension;
	private final BlockPos pos;

	public DimPos(String dimension, BlockPos pos) {
		this.dimension = dimension;
		this.pos = pos.toImmutable();
	}

	public DimPos(String dimension, int x, int y, int z) {
		this(dimension, new BlockPos(x, y, z));
	}
	
	public DimPos(World world, BlockPos pos) {
		this(world.provider.getDimensionType().getName(), pos);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DimPos) {
			DimPos other = (DimPos)obj;
			return other.dimension.equals(dimension) && other.pos.equals(pos);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(dimension, pos);
	}

	@Override
	public String toString() {
		return String.format("%s[%d, %d, %d]", dimension.toString(), pos.getX(), pos.getY(), pos.getZ());
	}

	public String getDimension() {
		return dimension;
	}

	public boolean is(World world) {
		return world.provider.getDimensionType().getName().equals(dimension);
	}

	public DimPos offset(String dimension) {
		return new DimPos(dimension, pos);
	}

	public BlockPos getBlockPos() {
		return pos;
	}

	public boolean is(BlockPos pos) {
		return pos.equals(this.pos);
	}

	public DimPos offset(EnumFacing dir) {
		return offset(dir, 1);
	}

	public DimPos offset(EnumFacing dir, int distance) {
		return new DimPos(dimension, pos.offset(dir, distance));
	}

	public DimPos offset(Axis axis) {
		return offset(axis, 1);
	}
	
	public DimPos offset(Axis axis, int distance) {
		int dx = AxisUtils.choose(axis, distance, 0, 0);
		int dy = AxisUtils.choose(axis, 0, distance, 0);
		int dz = AxisUtils.choose(axis, 0, 0, distance);

		return new DimPos(dimension, pos.add(dx, dy, dz));
	}

	public DimPos offset(int dx, int dy, int dz) {
		return new DimPos(dimension, pos.add(dx, dy, dz));
	}

	public NBTTagCompound toNbt() {
		NBTTagCompound nbt = new NBTTagCompound();

		nbt.setString("dim", dimension);
		nbt.setInteger("x", pos.getX());
		nbt.setInteger("y", pos.getY());
		nbt.setInteger("z", pos.getZ());

		return nbt;
	}

	public static DimPos fromNbt(NBTTagCompound nbt) {
		String dimension = nbt.getString("dim");
		int x = nbt.getInteger("x");
		int y = nbt.getInteger("y");
		int z = nbt.getInteger("z");

		return new DimPos(dimension, x, y, z);
	}
}
