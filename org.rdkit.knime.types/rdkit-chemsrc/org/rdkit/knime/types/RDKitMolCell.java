/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2010
 *  Novartis Institutes for BioMedical Research
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.rdkit.knime.types;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.RDKit.Int_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.node.NodeLogger;

/**
 * Default implementation of a Smiles Cell. This cell stores only the Smiles
 * string but does not do any checks or interpretation of the contained
 * information.
 *
 * @author Greg Landrum
 */
public class RDKitMolCell extends BlobDataCell implements StringValue,
        RDKitMolValue {

    /** Do not compress blobs, see {@link BlobDataCell#USE_COMPRESSION}. */
    @SuppressWarnings("hiding")
    public static final boolean USE_COMPRESSION = false;

    /**
     * Convenience access member for
     * <code>DataType.getType(RDKitMolCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    static final DataType TYPE = DataType.getType(RDKitMolCell.class);

    private static final long serialVersionUID = 0x1;

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitMolCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return SmilesValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return RDKitMolValue.class;
    }

    private static final RDKitMolSerializer SERIALIZER =
            new RDKitMolSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return a serializer for reading/writing cells of this kind
     * @see DataCell
     */
    public static final RDKitMolSerializer getCellSerializer() {
        return SERIALIZER;
    }

    private final String m_smilesString;

    private final ROMol m_mol;

	/** Living instance count (incremented in constructed, decremented in
	 * finalize() */
    private static final AtomicLong INSTANCE_COUNT = new AtomicLong();
    /** current time in millis we lasted reporting instance count on
     * debug. */
    private static final AtomicLong LAST_REPORT_TIME = new AtomicLong();
    /** live count after last manual GC. */
    private static final AtomicLong LAST_LIVE_COUNT_AFTER_GC =
        new AtomicLong();

    /** Package scope constructor that wraps the argument molecule.
     * @param mol The molecule to wrap.
     * @param canonsmiles RDKit canonical smiles for the molecule. 
     *        Leave this empty if you have any doubts how to generate it.
     */
    RDKitMolCell(final ROMol mol,final String canonSmiles) {
        if (mol == null) {
            throw new NullPointerException("Mol value must not be null.");
        }
        m_mol = mol;
        reportUsageAndFreeMemory(INSTANCE_COUNT.incrementAndGet());
        if(canonSmiles==""){
        	m_smilesString = RDKFuncs.MolToSmiles(m_mol, true);
        } else {
        	m_smilesString=canonSmiles;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        return m_smilesString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSmilesValue() {
        return m_smilesString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ROMol getMoleculeValue() {
        return m_mol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_smilesString.equals(((RDKitMolCell)dc).m_smilesString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_smilesString.hashCode();
    }

    /** Factory for (de-)serializing a RDKitMolCell. */
    private static class RDKitMolSerializer implements
            DataCellSerializer<RDKitMolCell> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final RDKitMolCell cell,
                final DataCellDataOutput output) throws IOException {
            // output.writeUTF(RDKFuncs.MolToBinary(cell.getMoleculeValue()));
            Int_Vect iv = RDKFuncs.MolToBinary(cell.getMoleculeValue());
            byte[] bytes = new byte[(int)iv.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte)iv.get(i);
            }
            output.writeInt(-1);
            output.writeUTF(cell.getSmilesValue());
            output.writeInt(bytes.length);
            output.write(bytes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RDKitMolCell deserialize(final DataCellDataInput input)
                throws IOException {
            int length = input.readInt();
            String smiles="";
            if(length<0){
            	smiles=input.readUTF();
            	length = input.readInt();
            }
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            Int_Vect iv = new Int_Vect(length);
            for (int i = 0; i < length; i++) {
                iv.set(i, bytes[i]);
            }
            ROMol m = RDKFuncs.MolFromBinary(iv);
            return new RDKitMolCell(m,smiles);
        }
    }

    private static void reportUsageAndFreeMemory(final long liveCount) {
        if (liveCount == 0) {
            LOGGER.debug("RDKit instance count: " + liveCount);
        }
        long last = LAST_REPORT_TIME.get();
        long now = System.currentTimeMillis();
        long diff = now - last;
        // do not report too often
        if (diff > 3000 && LAST_REPORT_TIME.compareAndSet(last, now)) {
            LOGGER.debug("RDKit instance count: " + liveCount);
        }
        // call GC if 10000 objects were created while no objects were
        // finalized
        final int createdObjectThreshold = 10000;
        long lastLiveCountAfterGC = LAST_LIVE_COUNT_AFTER_GC.get();
        if (lastLiveCountAfterGC > liveCount) {
            // decreasing number of living objects ... update field
            LAST_LIVE_COUNT_AFTER_GC.compareAndSet(
                    lastLiveCountAfterGC, liveCount);
        } else if (liveCount - lastLiveCountAfterGC > createdObjectThreshold) {
            // call GC if only if this thread successfully updates the value
            if (LAST_LIVE_COUNT_AFTER_GC.compareAndSet(
                    lastLiveCountAfterGC, Long.MAX_VALUE)) {
                LOGGER.debug("RDKit - calling GC to force finalizer ("
                        + liveCount + " living objects)");
                System.gc();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        m_mol.delete();
        reportUsageAndFreeMemory(INSTANCE_COUNT.decrementAndGet());
        super.finalize();
    }

}
