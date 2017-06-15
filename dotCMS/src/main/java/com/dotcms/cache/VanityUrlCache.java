package com.dotcms.cache;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;

/**
 * This cache is used to map the Vanity URLs path to the Vanity Url
 * content.
 * 
 * @author oswaldogallango
 */
public abstract class VanityUrlCache implements Cachable {

	/**
	 * Add or update in the cache the given Vanity URL
	 * based on given the given key
	 * @param key 
	 * @param vanityUrl
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract public VanityUrl add(String key, VanityUrl vanityUrl);

	/**
	 * Retrieves the Vanity URL associated to the given
	 * key
	 * @param Key
	 * @return VanityUrl
	 * @throws DotDataException
	 */
	abstract public VanityUrl get(String key);

	/**
	 * Removes all entries from cache
	 */
	abstract public void clearCache();

	/**
	 * This method removes the VanityUrl entry from the cache
	 * based on the key
	 * @param object
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract public void remove(String key);

}