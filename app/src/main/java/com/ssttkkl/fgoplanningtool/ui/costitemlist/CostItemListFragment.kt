package com.ssttkkl.fgoplanningtool.ui.costitemlist


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ssttkkl.fgoplanningtool.R
import com.ssttkkl.fgoplanningtool.data.Repo
import com.ssttkkl.fgoplanningtool.data.item.costItems
import com.ssttkkl.fgoplanningtool.data.plan.Plan
import com.ssttkkl.fgoplanningtool.resources.ResourcesProvider
import com.ssttkkl.fgoplanningtool.ui.requirementlist.RequirementListEntity
import com.ssttkkl.fgoplanningtool.ui.servantinfo.ServantInfoDialogFragment
import com.ssttkkl.fgoplanningtool.ui.utils.CommonRecViewItemDecoration
import kotlinx.android.synthetic.main.fragment_costitemlist.*

class CostItemListFragment : Fragment() {
    var plans: Collection<Plan> = listOf()
        set(value) {
            field = value
            (recView?.adapter as? CostItemListAdapter)?.data = generateEntities(plans)
            recView?.visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
        }

    private fun generateEntities(plans: Collection<Plan>): List<CostItemListEntity> {
        // key: item's codename
        // value: a set contains pairs, each stands for a servant requires this item.
        //        the first is servantID, the second is requirement.
        val map = HashMap<String, HashSet<Pair<Int, Long>>>()
        plans.forEach { plan ->
            plan.costItems.forEach { item ->
                if (!map.containsKey(item.codename))
                    map[item.codename] = HashSet()
                map[item.codename]!!.add(Pair(plan.servantId, item.count))
            }
        }

        val entities = map.map { (codename, req) ->
            var need = 0L
            req.forEach {
                need += it.second
            }

            val descriptor = ResourcesProvider.instance.itemDescriptors[codename]
            CostItemListEntity(name = descriptor?.localizedName ?: codename,
                    type = descriptor?.type,
                    need = need,
                    own = Repo.itemRepo[codename].count,
                    imgFile = descriptor?.imgFile,
                    codename = codename,
                    requirements = req.sortedBy { it.first }.map { (servantID, count) ->
                        val servant = ResourcesProvider.instance.servants[servantID]
                        RequirementListEntity(servantID = servantID,
                                name = servant?.localizedName ?: servantID.toString(),
                                requirement = count,
                                avatarFile = servant?.avatarFile)
                    })
        }
        return entities.groupBy { it.type }.toList().sortedBy { it.first } // group items and sort groups by type
                .map { it.second.sortedBy { ResourcesProvider.instance.itemRank[it.codename] } }.flatMap { it } // sort each group's items and flat
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_PLANS))
            plans = savedInstanceState.getParcelableArray(ARG_PLANS).map { it as Plan }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_costitemlist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recView.apply {
            adapter = CostItemListAdapter(context!!).apply {
                data = generateEntities(plans)
                setOnServantClickListener { gotoServantDetailUi(it) }
                if (savedInstanceState != null && savedInstanceState.containsKey(ARG_EXPANDED))
                    expendedPosition = savedInstanceState.getInt(ARG_EXPANDED)
            }
            layoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(CommonRecViewItemDecoration(context!!, false, true))
            visibility = if (plans.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArray(ARG_PLANS, plans.toTypedArray())
        outState.putInt(ARG_EXPANDED, (recView?.adapter as? CostItemListAdapter)?.expendedPosition
                ?: -1)
    }

    private fun gotoServantDetailUi(servantID: Int) {
        ServantInfoDialogFragment.newInstance(servantID)
                .show(childFragmentManager, ServantInfoDialogFragment::class.qualifiedName)
    }

    companion object {
        private const val ARG_PLANS = "plans"
        private const val ARG_EXPANDED = "expanded"
    }
}