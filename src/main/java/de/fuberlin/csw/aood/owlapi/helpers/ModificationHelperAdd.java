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
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import de.fuberlin.csw.aood.owlapi.OWLAspectAnd;
import de.fuberlin.csw.aood.owlapi.OWLAspectOr;

/**
 * This helper provides methods to handle axiom additions considering current aspects
 */
public class ModificationHelperAdd extends ModificationHelper {
	
	/**
	 * handle ontology change of type AddAxiom (update current aspect annotations)
	 * 
	 * @param change
	 * 			change of type AddAxiom
	 * @param annotation
	 * 		Annotation of type {@link OWLAspectAnd} or {@link OWLAspectOr} specifying current aspects
	 * @return status whether this change has been applied successfully (aspect annotations updated)
	 */
	public static List<OWLOntologyChange> handleChangeAddAxiom(OWLOntologyChange change, Annotation annotation) {
		String[] currentAspects = getCurrentAspectsAdd(annotation);
        List<OWLOntologyChange> result = handleAddAxiom(change.getAxiom(), change.getOntology(), currentAspects);//handleChangeAdd(change, currentAspects);
//	we have decided not to do this step
//		ModificationHelperPostprocess.postprocessChange(currentAspects, change);
		return result;
	}


	/**
	 * handle axiom addition (update current aspects)
	 * 
	 * @param userAx
	 * 			axiom which user has asked to add
	 * @param onto
	 * 			current ontology
	 * @param currentAspects
	 * 			aspect IRIs provided as String array
	 * @return status whether the change was applied successfully 
	 * 			(if aspect annotations were updated successfully)
	 */
	private static List<OWLOntologyChange> handleAddAxiom(OWLAxiom userAx, OWLOntology onto, String[] currentAspects) {
		// TRICKY ALGORITHM
		// Check all axioms for which is true:
		// set (real annos) = all annos on this ax which are not aspects
		// if anno (set of real annos) equals axiom user asked for : add aspects
		// else add new axiom with aspects
        List<OWLOntologyChange> changesList = null;
		OWLOntologyManager om = onto.getOWLOntologyManager();
		OWLDataFactory df = om.getOWLDataFactory();
		for (OWLAxiom similarAxiom : onto.getAxiomsIgnoreAnnotations(userAx)) {
			Set<OWLAnnotation> regularAnnotationsOnSimilarAxiom = findRegularAnnotations(df, similarAxiom.getAnnotations());
			Set<OWLAnnotation> regularAnnotationsOnUserAxiom = findRegularAnnotations(df, userAx.getAnnotations());
			if (regularAnnotationsOnSimilarAxiom.equals(regularAnnotationsOnUserAxiom)) {
				// add all the current aspects to this whole axiom now, 
				// regardless of which aspects it already has
				Set<OWLAnnotation> currentAspectAnnotations = createSetOfRelevantAnnotations(onto, currentAspects);
				OWLAxiom axiomToStay = similarAxiom.getAnnotatedAxiom(currentAspectAnnotations);
				om.removeAxiom(onto, similarAxiom);
                changesList = om.addAxiom(onto, axiomToStay);
			}				
		}
		if (changesList==null) {
			om.addAxiom(onto, associateAxiomWithAspects(onto, userAx, currentAspects));
		}
		return changesList;
	}
	
	/**
	 * combine all aspects specified in this annotation in one String array 
	 * since we dont care about the distinction between AND and OR in this case
	 * 
	 * @param annotation
	 * 			Annotation of type {@link OWLAspectAnd} or {@link OWLAspectOr} specifying current aspects
	 * @return String array of aspect IRIs (logically conjunct)
	 */
	private static String[] getCurrentAspectsAdd(Annotation annotation) {
		String[] currentAspects = new String[0];	
		if (annotation instanceof OWLAspectAnd) { // OWLAspectAnd
			currentAspects = ((OWLAspectAnd) annotation).value();			
		} else { // OWLAspectOr
			for (OWLAspectAnd aspectListAnd : ((OWLAspectOr) annotation).value()) {
				String[] someOfCurrentAspects = aspectListAnd.value();		
				currentAspects = concatenateTwoStringArrays(currentAspects, someOfCurrentAspects);		
			}
		}
		return currentAspects;
	}


}
