package org.nicolasgramlich.toolbox.adt.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * @author Nicolas Gramlich
 * @since Nov 20, 2012
 */
public class BitVector {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int TRUE = 1;
	public static final int FALSE = 0;

	// ===========================================================
	// Fields
	// ===========================================================

	private final int mSize;

	private final byte[] mBytes;

	// ===========================================================
	// Constructors
	// ===========================================================

	public BitVector(final int pSize) throws IllegalArgumentException {
		this(pSize, new byte[BitVector.calculateByteSize(pSize)]);
	}

	public BitVector(final byte[] pBytes) throws IllegalArgumentException, NullPointerException {
		this(pBytes.length * Byte.SIZE, pBytes);
	}

	public BitVector(final int pSize, final byte[] pBytes) throws IllegalArgumentException, NullPointerException {
		if (pBytes == null){
			throw new IllegalArgumentException("pBytes must not be null");
		}

		if (pSize > (pBytes.length * Byte.SIZE)) {
			throw new IllegalArgumentException("pBytes is too short.");
		}

		if (BitVector.calculateByteSize(pSize) < pBytes.length) {
			throw new IllegalArgumentException("pBytes is too long.");
		}

		this.mSize = pSize;

		this.mBytes = pBytes;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getSize() {
		return this.mSize;
	}

	public int getByteSize() {
		return this.mBytes.length;
	}

	public int getBit(final int pIndex) throws IllegalArgumentException {
		if ((pIndex < 0) || (pIndex >= this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final int byteIndex = BitVector.getByteIndex(pIndex);
		final int indexInByte = BitVector.getIndexInByte(pIndex);

		return BitVector.getBitInByte(this.mBytes[byteIndex], indexInByte);
	}

	public boolean getBitAsBoolean(final int pIndex) throws IllegalArgumentException {
		return this.getBit(pIndex) == BitVector.TRUE;
	}

	public int getBits(final int pIndex, final int pCount) throws IllegalArgumentException {
		if ((pIndex < 0) || (pIndex + pCount > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		int bits = 0;

		int bitsLeft = pCount;
		int index = pIndex;
		while (bitsLeft >= Byte.SIZE) {
			bits = bits << Byte.SIZE;
			bits |= (this.getByte(index)) & 0xFF;
			index += Byte.SIZE;
			bitsLeft -= Byte.SIZE;
		}

		for (int i = 0; i < bitsLeft; i++) {
			bits = bits << 1;
			if(this.getBit(index) == BitVector.TRUE) {
				bits |= 0x01;
			}
			index++;
		}

		return bits;
	}

	public void setBit(final int pIndex) {
		this.setBit(pIndex, true);
	}

	public void clearBit(final int pIndex) {
		this.setBit(pIndex, false);
	}

	public void setBit(final int pIndex, final boolean pTrue) {
		if ((pIndex < 0) || (pIndex >= this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final int byteIndex = BitVector.getByteIndex(pIndex);
		final int indexInByte = BitVector.getIndexInByte(pIndex);
		final byte oldByte = this.mBytes[byteIndex];

		final byte newByte = BitVector.setBitInByte(oldByte, indexInByte, pTrue);
		this.mBytes[byteIndex] = newByte;
	}

	public void setBits(final int pIndex, final byte pBits, final int pBitIndex, final int pBitCount) {
		if ((pIndex < 0) || ((pIndex + pBitCount) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		if ((pBitIndex < 0) || ((pBitIndex + pBitCount) > Byte.SIZE)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pBitIndex);
		}

		final int indexInByte = BitVector.getIndexInByte(pIndex);
		final int byteIndex = BitVector.getByteIndex(pIndex);
		if (indexInByte == 0) {
			/* Perfect match, easy get. */
			final byte newByte = BitVector.setBitsInByte(this.mBytes[byteIndex], 0, pBits, pBitIndex, pBitCount);
			this.mBytes[byteIndex] = newByte;
		} else {
			final byte highByte = this.mBytes[byteIndex];
			final int highByteBitCount = Math.min(pBitCount, Byte.SIZE - indexInByte);

			final byte newHighByte = BitVector.setBitsInByte(highByte, indexInByte, pBits, pBitIndex, highByteBitCount);
			this.mBytes[byteIndex] = newHighByte;

			if (highByteBitCount < pBitCount) {
				final byte lowByte = this.mBytes[byteIndex + 1];
				final int lowByteBitIndex = pBitIndex + highByteBitCount;
				final int lowByteBitCount = pBitCount - highByteBitCount;

				final byte newLowByte = BitVector.setBitsInByte(lowByte, 0, pBits, lowByteBitIndex, lowByteBitCount);
				this.mBytes[byteIndex + 1] = newLowByte;
			}
		}
	}

	public void setBits(final int pIndex, final short pBits, final int pBitIndex, final int pBitCount) {
		if ((pIndex < 0) || ((pIndex + pBitCount) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		if ((pBitIndex < 0) || ((pBitIndex + pBitCount) > Short.SIZE)) {
			throw new IllegalArgumentException("pBitIndex out of bounds: " + pBitIndex);
		}

		final int highByteBitCount = Math.min(pBitCount, Math.max(0, Byte.SIZE - pBitIndex));
		if (highByteBitCount != 0) {
			final byte highByte = (byte)((pBits >> (1 * Byte.SIZE)) & 0xFF);
			this.setBits(pIndex, highByte, pBitIndex, highByteBitCount);
		}

		if (pBitCount > highByteBitCount) {
			final byte lowByte = (byte)((pBits >> (0 * Byte.SIZE)) & 0xFF);
			final int lowByteBitCount = pBitCount - highByteBitCount;

			if(highByteBitCount == 0) {
				this.setBits(pIndex, lowByte, (pBitIndex - Byte.SIZE) % Byte.SIZE, lowByteBitCount);
			} else {
				this.setBits(pIndex + highByteBitCount, lowByte, 0, lowByteBitCount);
			}
		}
	}

	public void setBits(final int pIndex, final int pBits, final int pBitIndex, final int pBitCount) {
		if ((pIndex < 0) || ((pIndex + pBitCount) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		if ((pBitIndex < 0) || ((pBitIndex + pBitCount) > Integer.SIZE)) {
			throw new IllegalArgumentException("pBitIndex out of bounds: " + pBitIndex);
		}

		final int highShortBitCount = Math.min(pBitCount, Math.max(0, Short.SIZE - pBitIndex));
		if (highShortBitCount != 0) {
			final short highShort = (short)((pBits >> (1 * Short.SIZE)) & 0xFFFF);
			this.setBits(pIndex, highShort, pBitIndex, highShortBitCount);
		}

		if (pBitCount > highShortBitCount) {
			final short lowShort = (short)((pBits >> (0 * Short.SIZE)) & 0xFFFF);
			final int lowShortBitCount = pBitCount - highShortBitCount;

			if(highShortBitCount == 0) {
				this.setBits(pIndex, lowShort, (pBitIndex - Short.SIZE) % Short.SIZE, lowShortBitCount);
			} else {
				this.setBits(pIndex + highShortBitCount, lowShort, 0, lowShortBitCount);
			}
		}
	}

	public byte getByte(final int pIndex) {
		if ((pIndex < 0) || ((pIndex + Byte.SIZE) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final int indexInByte = BitVector.getIndexInByte(pIndex);
		final int byteIndex = BitVector.getByteIndex(pIndex);
		if (indexInByte == 0) {
			/* Perfect match, easy get. */
			return this.mBytes[byteIndex];
		} else {
			final byte highByte = this.mBytes[byteIndex];
			final byte lowByte = this.mBytes[byteIndex + 1];

			final int highBits = BitVector.getBitsInByte(highByte, indexInByte, Byte.SIZE - indexInByte);
			final int lowBits = BitVector.getBitsInByte(lowByte, 0, indexInByte);

			final int result = (highBits << indexInByte) + lowBits;

			return (byte)result;
		}
	}

	public final void setByte(final int pIndex, final byte pByte) {
		if ((pIndex < 0) || ((pIndex + Byte.SIZE) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final int indexInByte = BitVector.getIndexInByte(pIndex);
		final int byteIndex = BitVector.getByteIndex(pIndex);
		if (indexInByte == 0) {
			/* Perfect match, easy set. */
			this.mBytes[byteIndex] = pByte;
		} else {
			final byte highByte = this.mBytes[byteIndex];
			final byte lowByte = this.mBytes[byteIndex + 1];

			this.mBytes[byteIndex] = BitVector.setBitsInByte(highByte, indexInByte, pByte, 0, Byte.SIZE - indexInByte);
			this.mBytes[byteIndex + 1] = BitVector.setBitsInByte(lowByte, 0, pByte, Byte.SIZE - indexInByte, indexInByte);
		}
	}

	public short getShort(final int pIndex) {
		if ((pIndex < 0) || ((pIndex + Short.SIZE) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final int highByte = this.getByte(pIndex) & 0xFF;
		final int lowByte = this.getByte(pIndex + Byte.SIZE) & 0xFF;

		final short result = (short)((highByte << Byte.SIZE) | lowByte);

		return result;
	}

	public final void setShort(final int pIndex, final short pShort) {
		if ((pIndex < 0) || ((pIndex + Short.SIZE) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final byte highByte = (byte)((pShort >> Byte.SIZE) & 0xFF);
		final byte lowByte = (byte)(pShort & 0xFF);

		this.setByte(pIndex, highByte);
		this.setByte(pIndex + Byte.SIZE, lowByte);
	}

	public int getInt(final int pIndex) {
		if ((pIndex < 0) || ((pIndex + Integer.SIZE) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		final int highestByte = this.getByte(pIndex + (0 * Byte.SIZE)) & 0xFF;
		final int highByte = this.getByte(pIndex + (1 * Byte.SIZE)) & 0xFF;
		final int lowByte = this.getByte(pIndex + (2 * Byte.SIZE)) & 0xFF;
		final int lowestByte = this.getByte(pIndex + (3 * Byte.SIZE)) & 0xFF;

		final int result = (highestByte << (3 * Byte.SIZE)) | (highByte << (2 * Byte.SIZE)) | (lowByte << (1 * Byte.SIZE)) | lowestByte;

		return result;
	}

	public void setInt(final int pIndex, final int pInt) {
		if ((pIndex < 0) || ((pIndex + Integer.SIZE) > this.mSize)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		this.setByte(pIndex + (0 * Byte.SIZE), (byte)((pInt >> (3 * Byte.SIZE)) & 0xFF));
		this.setByte(pIndex + (1 * Byte.SIZE), (byte)((pInt >> (2 * Byte.SIZE)) & 0xFF));
		this.setByte(pIndex + (2 * Byte.SIZE), (byte)((pInt >> (1 * Byte.SIZE)) & 0xFF));
		this.setByte(pIndex + (3 * Byte.SIZE), (byte)((pInt >> (0 * Byte.SIZE)) & 0xFF));
	}

	public void clear() {
		this.fill((byte)0x00);
	}

	public void fill(final byte pByte) {
		Arrays.fill(this.mBytes, pByte);
	}

	public void save(final DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeInt(this.mSize);
		pDataOutputStream.write(this.mBytes);
	}

	public static BitVector load(final DataInputStream pDataInputStream) throws IOException {
		final int size = pDataInputStream.readInt();
		final byte[] bytes = new byte[BitVector.calculateByteSize(size)];
		pDataInputStream.readFully(bytes);

		return new BitVector(size, bytes);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append('[');

		for (int i = 0; i < this.mSize; i++) {
			if (this.getBit(i) == BitVector.TRUE) {
				stringBuilder.append('1');
			} else {
				stringBuilder.append('0');
			}

			if (((i % Byte.SIZE) == 7) && (i < (this.mSize - 1))) {
				stringBuilder.append(',').append(' ');
			}
		}

		stringBuilder.append(']');
		return stringBuilder.toString();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private static int getByteIndex(final int pIndex) {
		return pIndex / Byte.SIZE;
	}

	private static int getIndexInByte(final int pIndex) {
		return pIndex % Byte.SIZE;
	}

	private static int getBitInByte(final byte pByte, final int pIndex) throws IllegalArgumentException {
		return BitVector.getBitsInByte(pByte, pIndex, 1);
	}

	private static int getBitsInByte(final byte pByte, final int pIndex, final int pCount) throws IllegalArgumentException {
		if ((pIndex < 0) || ((pIndex + pCount) > Byte.SIZE)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		if ((pCount < 0) || (pCount > Byte.SIZE)) {
			throw new IllegalArgumentException("pBitCount out of bounds: " + pCount);
		}

		final int shift = Byte.SIZE - pIndex - pCount;
		final int shiftedByte = ((pByte & 0xFF) >> shift);

		final int mask = (0x01 << (pCount)) - 1;

		final int result = shiftedByte & mask;

		return result;
	}

	private static byte setBitInByte(final byte pByte, final int pIndex, final boolean pTrue) throws IllegalArgumentException {
		if ((pIndex < 0) || (pIndex >= Byte.SIZE)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		if (pTrue) {
			final int mask = (0x80 >> pIndex);

			final byte result = (byte)(pByte | mask);

			return result;
		} else {
			final int mask = 0xFF ^ (0x80 >> pIndex);

			final byte result = (byte)(pByte & mask);

			return result;
		}
	}

	public static byte setBitsInByte(final byte pByte, final int pIndex, final byte pBits, final int pBitIndex, final int pBitCount) throws IllegalArgumentException {
		if ((pIndex < 0) || ((pIndex + pBitCount) > Byte.SIZE)) {
			throw new IllegalArgumentException("pIndex out of bounds: " + pIndex);
		}

		if ((pBitIndex + pBitCount) > Byte.SIZE) {
			throw new IllegalArgumentException("pBitIndex out of bounds: " + pIndex);
		}

		if ((pBitCount < 0) || (pBitCount > Byte.SIZE)) {
			throw new IllegalArgumentException("pBitCount out of bounds: " + pIndex);
		}

		final int sizeMask = (0x01 << (pBitCount)) - 1;

		final int byteMask = (sizeMask << (Byte.SIZE - (pBitCount + pIndex))) ^ 0xFF;
		final int maskedByte = (pByte & byteMask) & 0xFF;

		final int bitMask = sizeMask << (Byte.SIZE - (pBitCount + pBitIndex));
		final int maskedBits = (pBits & bitMask) & 0xFF;

		final int shift = (pBitIndex - pIndex);
		final int shiftedBits;
		if (shift < 0) {
			shiftedBits = maskedBits >> -shift;
		} else {
			shiftedBits = maskedBits << shift;
		}

		final byte result = (byte)(maskedByte | shiftedBits);

		return result;
	}

	private static int calculateByteSize(final int pBitSize) {
		if (pBitSize < 0) {
			throw new IllegalArgumentException("pBitSize out of bounds: " + pBitSize);
		}

		if (BitVector.getIndexInByte(pBitSize) == 0) {
			return pBitSize / Byte.SIZE;
		} else {
			return 1 + (pBitSize / Byte.SIZE);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
