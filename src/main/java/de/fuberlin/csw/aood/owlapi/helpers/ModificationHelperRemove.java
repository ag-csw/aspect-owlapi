/*******************************************************************************
 * This file is part of the Corporate Smart Content Project at Freie Universitaet Berlin, Corporate Semantic Web Group.
 * 
 * This work has been partially supported by the "InnoProfile-Corporate Semantic Web" project funded by the German Federal Ministry of Education and Research (BMBF) and the BMBF Innovation Initiative for the New German Laender - Entrepreneurial Regions.
 * 
 * http://www.corporate-smart-content.de/
 * 
 * Freie Universitaet Berlin
 * Copyright (c) 2013-2016
 * 
 * Institut fuer Informatik
 * Working Group Corporate Semantic Web
 * Koenigin-Luise-Strasse 24-26
 * 14195 Berlin
 * 
 * http://www.mi.fu-berlin.de/en/inf/groups/ag-csw/
 ******************************************************************************/
package de.fuberlin.csw.aood.owlapi.helpers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import de.fuberlin.csw.aood.owlapi.OWLAspectAnd;
import de.fuberlin.csw.aood.owlapi.OWLAspectOr;

/**
 * This helper provides methods to handle axiom removals considering current aspects
 */
public class ModificationHelperRemove extends ModificationHelper {
	
	// used by Modif.Helper
	/**
	 * handles change of type RemoveAxiom which was asked to be applied under current aspects
	 * 
	 * @param change
	 * 			change of type RemoveAxiom
	 * @param annotation
	 * 			Annotation of type {@link OWLAspectAnd} or {@link OWLAspectOr} specifying current aspects
	 * @return List of changes (this change) if aspect annotations were updated
	 */
	public static List<OWLOntologyChange> handleChangeRemoveAxiomReturnListOfChanges(OWLOntologyChange change, Annotation annotation) {
		ChangeApplied chgApplied = handleChangeRemoveAxiom(change, annotation);
		return produceListOfChangesFromChangeApplied(change.getOntology(), change.getAxiom(), chgApplied);
	}
	
	// used by HelperFacade
	/**
	 * handles axiom which user asked to remove under current aspects
	 * 
	 * @param axiom
	 * 			axiom to be removed (aspect annotations to be updated)
	 * @param ontology
	 * 			current ontology
	 * @param annotation
	 * 			Annotation of type {@link OWLAspectAnd} or {@link OWLAspectOr} specifying current aspects
	 * @return List of changes (RemoveAxiom this axiom) if aspect annotations were updated
	 */
	public static List<OWLOntologyChange> handleRemoveAxiomReturnChanges(
			OWLAxiom axiom, OWLOntology ontology, Annotation annotation) {	
		ChangeApplied chgApplied = handleChangeRemoveAxiom(new RemoveAxiom(ontology, axiom), annotation);
		return produceListOfChangesFromChangeApplied(ontology, axiom, chgApplied);
	}
	
	/**
	 * handle axiom asked to be removed under current aspects
	 * 
	 * @param change
	 * 			change of type RemoveAxioms
	 * @param annotation
	 * 			Annotation of type {@link OWLAspectAnd} or {@link OWLAspectOr} specifying current aspects
	 * @return ChangeApplied: status if aspects updated successfully
	 */
	public static ChangeApplied handleChangeRemoveAxiom(OWLOntologyChange change, Annotation annotation) {
		ChangeApplied result = ChangeApplied.UNSUCCESSFULLY;
		OWLOntology onto = change.getOntology();
		OWLAxiom userAx = change.getAxiom();
		if (annotation instanceof OWLAspectAnd) { // OWLAspectAnd
			String[] currentAspects = ((OWLAspectAnd) annotation).value();
			result = handleRemoveAxiom(userAx, onto, currentAspects);
			// 	not using it anymore, deprecated
			//	ModificationHelperPostprocess.postprocessChange(currentAspects, change);
			return result;		
		} else { // OWLAspectOr				
			for (OWLAxiom similarAxiom : getSimilarAxioms(userAx, onto)) {
				// for every And-List check first if Axiom has all aspects in this list
				// If yes add those aspects to a helper list
				// After all And-Lists have been checked, disassociate this axiom from the aspects in the helper list.
				// (if those are the last aspects in this axiom, then the axiom just stays aspectless)	
				String[] aspectsToRemove = new String[0];
				for (OWLAspectAnd aspectListAnd : ((OWLAspectOr) annotation).value()) {
					String[] someOfCurrentAspects = aspectListAnd.value();
					if (hasAllAspects(similarAxiom, onto, someOfCurrentAspects)) {
						aspectsToRemove = concatenateTwoStringArrays(aspectsToRemove, someOfCurrentAspects);
					}
				}
				result = disassociateAxiomFromCurrentAspects(similarAxiom, onto, aspectsToRemove);
				// Deprecated method, not using it anymore
				// ModificationHelperPostprocess.postprocessAddOrRemoveAxiom(onto, userAx, currentAspects, false);
			}			
		}
		return result;
	}
	
	/**
	 * check if an axiom similar to this axiom exists in this ontology (which differ only in aspect annotations)
	 * and if yes, removes current aspect annotations from this axiom 
	 * 
	 * @param axiom
	 * 			axiom which user asked to remove
	 * @param onto
	 * 			current ontology
	 * @param currentAspects
	 * 			current aspects
	 * @return status of type ChangeApplied: 
	 * 			successfully, if aspect annotations were removed,
	 * 			unsuccessfully otherwise
	 */
	private static ChangeApplied handleRemoveAxiom(OWLAxiom userAx, OWLOntology onto, String[] currentAspects) {
		// search for similar axioms (differing only in aspect annotations)
		// and delete aspects from such axioms
		// otherwise let it be. (return unsuccessfully)
		ChangeApplied chgApplied = ChangeApplied.UNSUCCESSFULLY;	
		for (OWLAxiom similarAxiom : getSimilarAxioms(userAx, onto)) {
			if (hasAllAspects(similarAxiom, onto, currentAspects)) {
				chgApplied = disassociateAxiomFromCurrentAspects(similarAxiom, onto, currentAspects);
			}
		}			
		return chgApplied;
	}
	
	/**
	 * removes current aspect annotations from this axiom 
	 * (aka replacing this axiom with its copy without current aspect annotations).
	 * 
	 * @param axiom
	 * 			axiom which maybe has some current aspects
	 * @param onto
	 * 			current ontology
	 * @param currentAspects
	 * 			current aspects
	 * @return status of type ChangeApplied: 
	 * 			successfully, if aspect annotations have been updated,
	 * 			unsuccessfully otherwise
	 */
	private static ChangeApplied disassociateAxiomFromCurrentAspects(OWLAxiom axiom, OWLOntology onto, String[] currentAspects) {
		ChangeApplied chgApplied;
		OWLOntologyManager om = onto.getOWLOntologyManager();
		Set<OWLAnnotation> relevantAspectAnnotations = createSetOfRelevantAnnotations(onto, currentAspects);
		OWLAxiom axBase = axiom.getAxiomWithoutAnnotations();
		// delete relevant aspect annotations from similarAxiom: (3 steps)
		// 1. find all annotations which are not current aspects
		Set<OWLAnnotation> annosToStay = new HashSet<OWLAnnotation>();
		for (OWLAnnotation anno : axiom.getAnnotations()) {
			if (!relevantAspectAnnotations.contains(anno)) {
				annosToStay.add(anno);
			}
		}
		// 2. add those annotations which are not current aspects to axToStay
		OWLAxiom axiomToStay = axBase.getAnnotatedAxiom(annosToStay);
		// 3. replace axiom with axiomToStay
		chgApplied = om.applyChange(new RemoveAxiom(onto, axiom));
		om.addAxiom(onto, axiomToStay);
		return chgApplied;
	}
	
	/**
	 * checks if argument chgApplied equals SUCCESSFULLY, 
	 * and if yes create list of changes of type RemoveAxiom from this axiom
	 * 
	 * @param ontology
	 * 			ontology
	 * @param axiom 
	 * 			axiom to be transformed to RemoveAxiom change (maybe)
	 * @param chgApplied 
	 * 			Status whether a change has been applied successfully
	 * @return list of changes according to chgApplied
	 */
	private static List<OWLOntologyChange> produceListOfChangesFromChangeApplied(
			OWLOntology ontology, OWLAxiom axiom, ChangeApplied chgApplied) {
		// needed to match signature of removeAxiom()
		List<OWLOntologyChange> changesApplied = new ArrayList<OWLOntologyChange>();
		if(chgApplied.equals(ChangeApplied.SUCCESSFULLY)) {
			changesApplied.add(new RemoveAxiom(ontology, axiom));
		}
		return changesApplied;
	}

}
