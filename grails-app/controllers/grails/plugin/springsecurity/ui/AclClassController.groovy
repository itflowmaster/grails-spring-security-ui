/* Copyright 2009-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.springsecurity.ui

import grails.converters.JSON

import org.springframework.dao.DataIntegrityViolationException

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclClassController extends AbstractS2UiController {

	def create() {
		[aclClass: lookupClass().newInstance(params)]
	}

	def save() {
		def aclClass = lookupClass().newInstance(params)
		if (!aclClass.save(flush: true)) {
			render view: 'create', model: [aclClass: aclClass]
			return
		}

		flash.message = "${message(code: 'default.created.message', args: [message(code: 'aclClass.label', default: 'AclClass'), aclClass.id])}"
		redirect action: 'edit', id: aclClass.id
	}

	def edit() {
		def aclClass = findById()
		if (!aclClass) return

		[aclClass: aclClass]
	}

	def update() {
		def aclClass = findById()
		if (!aclClass) return
		if (!versionCheck('aclClass.label', 'AclClass', aclClass, [aclClass: aclClass])) {
			return
		}

		if (!springSecurityUiService.updateAclClass(aclClass, params.className)) {
			render view: 'edit', model: [aclClass: aclClass]
			return
		}

		flash.message = "${message(code: 'default.updated.message', args: [message(code: 'aclClass.label', default: 'AclClass'), aclClass.id])}"
		redirect action: 'edit', id: aclClass.id
	}

	def delete() {
		def aclClass = findById()
		if (!aclClass) return

		try {
			springSecurityUiService.deleteAclClass aclClass
			flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'aclClass.label', default: 'AclClass'), params.id])}"
			redirect action: 'search'
		}
		catch (DataIntegrityViolationException e) {
			flash.error = "${message(code: 'default.not.deleted.message', args: [message(code: 'aclClass.label', default: 'AclClass'), params.id])}"
			redirect action: 'edit', id: params.id
		}
	}

	def search() {}

	def aclClassSearch() {

		boolean useOffset = params.containsKey('offset')
		setIfMissing 'max', 10, 100
		setIfMissing 'offset', 0

		Integer max = params.int('max')
		Integer offset = params.int('offset')

		def cs = lookupClass().createCriteria()
		def results = cs.list(max: max,offset: offset) {
			firstResult: offset
			maxResults: max
			if (params['className']) {
				ilike('className','%' + params['className'] + '%')
			}
			if (params.sort) {
				order(params.sort,params.order ?: 'ASC')
			}
		}
		def model = [results: results, totalCount: results.totalCount, searched: true]

		// add query params to model for paging
		for (name in ['className']) {
		 	model[name] = params[name]
		}

		render view: 'search', model: model
	}

	/**
	 * Ajax call used by autocomplete textfield.
	 */
	def ajaxAclClassSearch() {

		def jsonData = []

		if (params.term?.length() > 2) {
			String className = params.term
			setIfMissing 'max', 10, 100
			def cs = lookupClass().createCriteria()
			def results = cs.list(max: params.int('max')) {
				maxResults: params.int('max')
				ilike('className','%' + className + '%')
				order('className','DESC')
				projections{
					distinct('className')
				}
			}

			for (result in results) {
				jsonData << [value: result]
			}
		}

		render text: jsonData as JSON, contentType: 'text/plain'
	}

	protected findById() {
		def aclClass = lookupClass().get(params.id)
		if (!aclClass) {
			flash.error = "${message(code: 'default.not.found.message', args: [message(code: 'aclClass.label', default: 'AclClass'), params.id])}"
			redirect action: 'search'
		}

		aclClass
	}

	protected String lookupClassName() {
		'grails.plugin.springsecurity.acl.AclClass'
	}

	protected Class<?> lookupClass() {
		grailsApplication.getDomainClass(lookupClassName()).clazz
	}
}
