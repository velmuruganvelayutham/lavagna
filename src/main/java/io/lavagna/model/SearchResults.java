/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lavagna.model;

import java.util.List;

import lombok.Getter;

@Getter
public class SearchResults {

	private final List<CardFullWithCounts> found;
	private final int count;
	private final int currentPage;
	private final int countPerPage;
	private final int totalPages;
	private final boolean paginate;

	public SearchResults(List<CardFullWithCounts> found, int count, int currentPage, int countPerPage, boolean paginate) {
		this.found = found;
		this.count = count;
		this.currentPage = currentPage;
		this.countPerPage = countPerPage;
		this.totalPages = paginate ? ((count + countPerPage - 1) / countPerPage) : 1;
		this.paginate = paginate;
	}
}
