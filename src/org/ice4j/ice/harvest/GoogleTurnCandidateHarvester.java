/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 * Maintained by the SIP Communicator community (http://sip-communicator.org).
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.ice4j.ice.harvest;

import org.ice4j.*;
import org.ice4j.ice.*;

/**
 * Implements a <tt>CandidateHarvester</tt> which gathers Google TURN dialect
 * <tt>Candidate</tt>s for a specified {@link Component}.
 *
 * @author Sebastien Vincent
 */
public class GoogleTurnCandidateHarvester
    extends StunCandidateHarvester
{
    /**
     * Initializes a new <tt>GoogleTurnCandidateHarvester</tt> instance which
     * is to work with a specific Google TURN server.
     *
     * @param turnServer the <tt>TransportAddress</tt> of the TURN server the
     * new instance is to work with
     */
    public GoogleTurnCandidateHarvester(TransportAddress turnServer)
    {
        this(turnServer, null);
    }

    /**
     * Initializes a new <tt>GoogleTurnCandidateHarvester</tt> instance which is
     * to work with a specific TURN server using a specific username for the
     * purposes of the STUN short-term credential mechanism.
     *
     * @param turnServer the <tt>TransportAddress</tt> of the TURN server the
     * new instance is to work with
     * @param shortTermCredentialUsername the username to be used by the new
     * instance for the purposes of the STUN short-term credential mechanism or
     * <tt>null</tt> if the use of the STUN short-term credential mechanism is
     * not determined at the time of the construction of the new instance
     */
    public GoogleTurnCandidateHarvester(TransportAddress turnServer,
            String shortTermCredentialUsername)
    {
        super(turnServer, shortTermCredentialUsername);
    }

    /**
     * Creates a new <tt>GoogleTurnCandidateHarvest</tt> instance which is to
     * perform TURN harvesting of a specific <tt>HostCandidate</tt>.
     *
     * @param hostCandidate the <tt>HostCandidate</tt> for which harvesting is
     * to be performed by the new <tt>TurnCandidateHarvest</tt> instance
     * @return a new <tt>GoogleTurnCandidateHarvest</tt> instance which is to
     * perform TURN harvesting of the specified <tt>hostCandidate</tt>
     * @see StunCandidateHarvester#createHarvest(HostCandidate)
     */
    @Override
    protected GoogleTurnCandidateHarvest createHarvest(
            HostCandidate hostCandidate)
    {
        return new GoogleTurnCandidateHarvest(this, hostCandidate);
    }
}
